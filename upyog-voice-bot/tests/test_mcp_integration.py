"""Tests for the voice bot's calls into upyog-mcp-server. HTTP is mocked."""

import json
import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from clients.upyog_mcp_client import McpCallError, McpUnavailable, UpyogMcpClient, _decode_body, _tool_payload
from clients import mcp_agent


class _Response:
    def __init__(self, body, status=200, headers=None, text=None):
        self.status_code = status
        self.headers = {"Content-Type": "application/json", "Mcp-Session-Id": "sess-1"}
        if headers:
            self.headers.update(headers)
        self._body = body
        self.text = text if text is not None else json.dumps(body)

    def json(self):
        return self._body


def _tool_response(payload):
    return _Response({
        "jsonrpc": "2.0",
        "id": 2,
        "result": {"content": [{"type": "text", "text": json.dumps(payload)}]},
    })


class DecodeTests(unittest.TestCase):
    def test_sse_body_uses_data_line(self):
        response = _Response({}, text='event: message\ndata: {"jsonrpc":"2.0","result":{"ok":true}}\n')
        response.headers["Content-Type"] = "text/event-stream"
        self.assertEqual(_decode_body(response)["result"]["ok"], True)

    def test_tool_text_is_parsed_as_json(self):
        parsed = _tool_payload({"content": [{"type": "text", "text": '{"items":[]}'}]})
        self.assertEqual(parsed, {"items": []})


class ClientTests(unittest.TestCase):
    def test_missing_token_is_rejected(self):
        client = UpyogMcpClient("http://mcp.test/mcp")
        with self.assertRaises(McpCallError) as caught:
            client.call_tool("search", {}, "")
        self.assertEqual(caught.exception.code, "AUTHENTICATION_REQUIRED")

    @patch("clients.upyog_mcp_client.requests.post")
    def test_tool_call_sends_auth_header_and_name(self, post):
        post.side_effect = [
            _Response({"jsonrpc": "2.0", "id": 1, "result": {"protocolVersion": "2025-03-26"}}),
            _Response({}),
            _tool_response({"items": [{"bookingNo": "ADV-1"}]}),
        ]
        client = UpyogMcpClient("http://mcp.test/mcp")
        result = client.call_tool("search", {"service": "advertisement"}, "citizen-token")
        self.assertEqual(result["items"][0]["bookingNo"], "ADV-1")
        tool_call = post.call_args_list[2]
        self.assertEqual(tool_call.kwargs["headers"]["auth-token"], "citizen-token")
        self.assertEqual(tool_call.kwargs["json"]["method"], "tools/call")
        self.assertEqual(tool_call.kwargs["json"]["params"]["name"], "search")
        self.assertNotIn("citizen-token", json.dumps(tool_call.kwargs["json"]))

    @patch("clients.upyog_mcp_client.requests.post")
    def test_business_error_is_raised(self, post):
        post.side_effect = [
            _Response({"jsonrpc": "2.0", "id": 1, "result": {}}),
            _Response({}),
            _tool_response({"code": "NOT_AUTHORIZED", "message": "Not allowed", "retryable": False}),
        ]
        client = UpyogMcpClient("http://mcp.test/mcp")
        with self.assertRaises(McpCallError) as caught:
            client.call_tool("search", {"service": "pgr"}, "token")
        self.assertEqual(caught.exception.code, "NOT_AUTHORIZED")

    @patch("clients.upyog_mcp_client.requests.post")
    def test_prepare_token_is_not_an_error(self, post):
        post.side_effect = [
            _Response({"jsonrpc": "2.0", "id": 1, "result": {}}),
            _Response({}),
            _tool_response({"confirmationToken": "abc", "readyForConfirmation": True}),
        ]
        client = UpyogMcpClient("http://mcp.test/mcp")
        result = client.call_tool("prepare_action", {"service": "pgr"}, "token")
        self.assertEqual(result["confirmationToken"], "abc")

    @patch("clients.upyog_mcp_client.requests.post")
    def test_connection_failure(self, post):
        import requests
        post.side_effect = requests.ConnectionError("down")
        client = UpyogMcpClient("http://mcp.test/mcp")
        with self.assertRaises(McpUnavailable):
            client.call_tool("search", {}, "token")


class AgentTests(unittest.TestCase):
    def setUp(self):
        self.user = {"tenantId": "pg.citya", "mobileNumber": "9999999999"}
        self.auth = patch.object(mcp_agent, "_auth", return_value=("citizen-token", self.user))
        self.auth.start()
        self.city = patch.object(mcp_agent, "_city_tenant", return_value="pg.citya")
        self.state = patch.object(mcp_agent, "_state_tenant", return_value="pg")
        self.city.start()
        self.state.start()

    def tearDown(self):
        self.auth.stop()
        self.city.stop()
        self.state.stop()

    def test_grievance_create_prepares_then_confirms(self):
        client = MagicMock()
        client.call_tool.side_effect = [
            {"confirmationToken": "tok-1", "readyForConfirmation": True},
            {"items": [{"service": {"serviceRequestId": "PGR-9", "applicationStatus": "PENDINGFORASSIGNMENT"}}]},
        ]
        with patch.object(mcp_agent, "_client", return_value=client):
            text = mcp_agent.pgr_create({
                "phone_number": "9999999999",
                "category_code": "Garbage",
                "description": "lane is dirty",
                "locality": "Preet Nagar",
                "locality_code": "JLC478",
            }, "9999999999")
        self.assertIn("PGR-9", text)
        self.assertEqual(client.call_tool.call_args_list[0].args[0], "prepare_action")
        self.assertEqual(client.call_tool.call_args_list[1].args[0], "confirm_action")
        self.assertEqual(client.call_tool.call_args_list[1].args[1]["confirmationToken"], "tok-1")
        prepare_payload = client.call_tool.call_args_list[0].args[1]["payload"]
        self.assertNotIn("RequestInfo", prepare_payload)
        self.assertNotIn("authToken", prepare_payload)

    def test_grievance_search_maps_projected_items(self):
        client = MagicMock()
        client.call_tool.return_value = {
            "items": [{"service": {"serviceRequestId": "PGR-1", "applicationStatus": "RESOLVED", "serviceCode": "Garbage"}}]
        }
        with patch.object(mcp_agent, "_client", return_value=client):
            rows = mcp_agent.pgr_search("9999999999", None)
        self.assertEqual(rows[0]["serviceRequestId"], "PGR-1")
        filters = client.call_tool.call_args.args[1]["filters"]
        self.assertEqual(filters["tenantId"], "pg.citya")
        self.assertEqual(filters["mobileNumber"], "9999999999")

    def test_empty_bill(self):
        client = MagicMock()
        client.call_tool.return_value = {"hasPendingBill": False, "billingResponse": {"Bill": []}}
        with patch.object(mcp_agent, "_client", return_value=client):
            text = mcp_agent.bill_text("ADV-1", "9999999999")
        self.assertIn("Could not find a bill", text)
        args = client.call_tool.call_args.args[1]
        self.assertEqual(args["businessService"], "adv-services")
        self.assertEqual(args["consumerCode"], "ADV-1")

    def test_unknown_master_is_rejected(self):
        with self.assertRaises(McpCallError) as caught:
            mcp_agent.master_names("NotAModule", "Anything")
        self.assertEqual(caught.exception.code, "MASTER_NOT_ALLOWED")

    def test_latest_booking_keeps_one_row(self):
        client = MagicMock()
        client.call_tool.return_value = {"items": [{"bookingNo": "ADV-1"}, {"bookingNo": "ADV-2"}]}
        with patch.object(mcp_agent, "_client", return_value=client):
            raw = mcp_agent.search_bookings("9999999999", None, None, True)
        self.assertEqual(json.loads(raw), [{"bookingNo": "ADV-1"}])

    def test_unreachable_server_message(self):
        self.assertIn("not reachable", mcp_agent.friendly(McpUnavailable("down"), "fallback"))


if __name__ == "__main__":
    unittest.main()

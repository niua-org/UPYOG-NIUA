"""
Maps the voice-bot workflows onto upyog-mcp-server tools.

Creates go through prepare_action and then confirm_action. The voice workflow
already asked the citizen to confirm before these functions run.
"""

import json
import logging
from typing import Any, Dict, List, Optional

from clients.upyog_mcp_client import McpCallError, McpUnavailable, UpyogMcpClient

logger = logging.getLogger(__name__)

_MODULE_SERVICE = {
    "Advertisement": "advertisement",
    "RAINMAKER-PGR": "pgr",
    "CHB": "venue-booking",
}


def _client() -> UpyogMcpClient:
    from clients.upyog_client import _mcp_client
    return _mcp_client()


def _auth(phone: Optional[str]):
    from clients.upyog_client import _mcp_auth
    return _mcp_auth(phone)


def _city_tenant(user_info: Dict[str, Any]) -> str:
    from clients.upyog_client import MDMS_TENANT_ID, get_current_base_url, get_current_environment_config
    env_cfg = get_current_environment_config(get_current_base_url())
    return user_info.get("tenantId") or env_cfg.get("tenant_id") or MDMS_TENANT_ID


def _state_tenant(user_info: Dict[str, Any]) -> str:
    from clients.upyog_client import get_current_base_url, get_current_environment_config
    env_cfg = get_current_environment_config(get_current_base_url())
    configured = env_cfg.get("state_tenant")
    if configured:
        return configured
    city = _city_tenant(user_info)
    return city.split(".")[0] if city else "pg"


def pgr_categories(phone: Optional[str]) -> dict:
    """Complaint types from lookup_master, grouped the way the grievance screen expects."""
    token, user = _auth(phone)
    result = _client().call_tool("lookup_master", {
        "service": "pgr",
        "master": "ServiceDefs",
        "tenantId": _state_tenant(user),
    }, token)
    defs = (((result.get("data") or {}).get("RAINMAKER-PGR") or {}).get("ServiceDefs") or [])
    structured: Dict[str, List[dict]] = {}
    for item in defs:
        if not item.get("active", True):
            continue
        menu = item.get("menuPath") or "Others"
        structured.setdefault(menu, []).append({
            "name": item.get("name", ""),
            "code": item.get("serviceCode", ""),
        })
    return structured


def pgr_search(phone: Optional[str], complaint_id: Optional[str]) -> List[dict]:
    """Search the signed-in citizen's grievances. A ticket id narrows the filter."""
    token, user = _auth(phone)
    filters = {"tenantId": _city_tenant(user)}
    mobile = user.get("mobileNumber")
    if complaint_id:
        filters["serviceRequestId"] = complaint_id.strip()
    elif mobile:
        filters["mobileNumber"] = mobile
    result = _client().call_tool("search", {
        "service": "pgr",
        "filters": filters,
        "page": 0,
        "size": 10,
    }, token)
    rows = []
    for item in result.get("items") or []:
        service = item.get("service") or item
        rows.append({
            "serviceRequestId": service.get("serviceRequestId"),
            "bookingNo": service.get("serviceRequestId"),
            "serviceCode": service.get("serviceCode"),
            "status": service.get("applicationStatus") or "Submitted",
            "applicationStatus": service.get("applicationStatus") or "Submitted",
            "description": service.get("description"),
            "locality": "N/A",
            "filed_on": "N/A",
            "bookingDate": "N/A",
        })
    return rows[:4]


def pgr_create(details: Dict[str, Any], phone: Optional[str]) -> str:
    """Register a grievance. The voice flow has already collected an explicit yes."""
    token, user = _auth(phone or details.get("phone_number"))
    tenant = _city_tenant(user)
    payload = {
        "tenantId": tenant,
        "serviceCode": details.get("category_code", ""),
        "description": details.get("description", ""),
        "priority": "HIGH",
        "address": {
            "tenantId": tenant,
            "city": details.get("locality") or "",
            "locality": {
                "code": details.get("locality_code", ""),
                "name": details.get("locality", ""),
            },
        },
    }
    confirmed = _prepare_and_confirm("pgr", "create", payload, token)
    items = confirmed.get("items") or []
    ticket = None
    if items:
        service = items[0].get("service") or items[0]
        ticket = service.get("serviceRequestId")
    if ticket:
        return (
            "**Complaint Registered Successfully**\n\n"
            f"- **Ticket Number:** `{ticket}`\n"
            f"- **Status:** Submitted\n\n"
        )
    if confirmed.get("code"):
        return confirmed.get("message") or "The complaint could not be registered."
    return (
        "**Complaint Submitted**\n\n"
        "Your complaint was sent. A ticket number was not returned. "
        "Check My Complaints in the UPYOG portal."
    )


def master_names(module_name: str, master_name: str, phone: Optional[str] = None) -> List[str]:
    """Dropdown labels from an allow-listed MDMS master. Unknown modules stay on the gateway."""
    service = _MODULE_SERVICE.get(module_name)
    if not service:
        raise McpCallError("MASTER_NOT_ALLOWED", f"{module_name} is not on the MCP server.", False)
    token, user = _auth(phone)
    result = _client().call_tool("lookup_master", {
        "service": service,
        "master": master_name,
        "tenantId": _state_tenant(user),
    }, token)
    module_data = (result.get("data") or {}).get(module_name) or {}
    items = module_data.get(master_name) or []
    names = []
    for item in items:
        if isinstance(item, str):
            names.append(item)
            continue
        if item.get("active", True):
            label = (item.get("name") or item.get("code") or "").strip()
            if label:
                names.append(label)
    return names


def search_bookings(mobile_number: str, booking_no: Optional[str], status: Optional[str], latest: bool) -> str:
    """Advertisement booking search. Returns the JSON string the booking workflow already parses."""
    token, user = _auth(mobile_number)
    filters = {"tenantId": _city_tenant(user), "mobileNumber": mobile_number}
    if booking_no:
        filters["bookingNo"] = booking_no
    if status:
        filters["status"] = status
    result = _client().call_tool("search", {
        "service": "advertisement",
        "filters": filters,
        "page": 0,
        "size": 4,
    }, token)
    items = result.get("items") or []
    if latest and items:
        items = items[:1]
    return json.dumps(items)


def bill_text(booking_no: str, mobile_number: Optional[str]) -> str:
    """Read a pending advertisement bill. The amount comes from billing-service, not from this bot."""
    token, user = _auth(mobile_number)
    result = _client().call_tool("get_pending_bill", {
        "businessService": "adv-services",
        "consumerCode": booking_no,
        "tenantId": _city_tenant(user),
    }, token)
    if not result.get("hasPendingBill"):
        return "Could not find a bill for this booking."
    bills = (result.get("billingResponse") or {}).get("Bill") or []
    if not bills:
        return "Could not find a bill for this booking."
    amount = bills[0].get("totalAmount")
    return f"Total Estimated Amount: ₹{amount}"


def create_advertisement(details: Dict[str, Any]) -> str:
    """Create an advertisement booking. Documents that are only file-store ids are not sent."""
    from clients.upyog_client import _bd
    token, user = _auth(details.get("mobileNumber"))
    tenant = _city_tenant(user)
    selected = details.get("selected_slots") or [{"date": details.get("start_date", "")}]
    cart = []
    for slot in selected:
        cart.append({
            "addType": slot.get("type", details.get("addType", "")),
            "faceArea": slot.get("area", details.get("faceArea", "")),
            "location": details.get("location", ""),
            "nightLight": str(slot.get("light", details.get("nightLight", "No"))).lower() in ("yes", "true"),
            "bookingDate": slot.get("date", details.get("start_date", "")),
            "bookingFromTime": _bd.get("booking_from_time"),
            "bookingToTime": _bd.get("booking_to_time"),
        })
    payload = {
        "tenantId": tenant,
        "cartDetails": cart,
        "applicantDetail": {
            "applicantName": details.get("applicantName", "Unknown"),
            "applicantMobileNo": details.get("mobileNumber", ""),
            "applicantEmailId": details.get("emailId", ""),
        },
        "address": {
            "addressLine1": details.get("address") or "",
            "city": details.get("city") or _bd.get("default_city") or "",
            "pincode": details.get("pincode") or _bd.get("default_pincode") or "",
        },
    }
    confirmed = _prepare_and_confirm("advertisement", "create", payload, token)
    items = confirmed.get("items") or []
    booking_no = None
    if items:
        booking_no = items[0].get("bookingNo") or (items[0].get("bookingApplication") or {}).get("bookingNo")
    if booking_no:
        return f"Booking successfully created! Application Number: {booking_no}"
    if confirmed.get("code"):
        return confirmed.get("message") or "The booking could not be created."
    return "The booking was sent, but no application number was returned."


def _prepare_and_confirm(service: str, operation: str, payload: Dict[str, Any], token: str) -> Dict[str, Any]:
    """prepare_action never writes. confirm_action sends only the body stored in the token."""
    client = _client()
    prepared = client.call_tool("prepare_action", {
        "service": service,
        "operation": operation,
        "payload": payload,
    }, token)
    confirmation = prepared.get("confirmationToken")
    if not confirmation:
        logger.info("MCP prepare did not return a token: %s", prepared.get("code"))
        return prepared
    logger.info("MCP prepared %s %s; confirming after the citizen already agreed in the voice flow", service, operation)
    return client.call_tool("confirm_action", {"confirmationToken": confirmation}, token)


def friendly(exc: Exception, fallback: str) -> str:
    if isinstance(exc, McpCallError):
        return exc.args[0] or fallback
    if isinstance(exc, McpUnavailable):
        return "The UPYOG assistant service is not reachable. Please try again shortly."
    logger.error("MCP agent call failed: %s", exc, exc_info=True)
    return fallback

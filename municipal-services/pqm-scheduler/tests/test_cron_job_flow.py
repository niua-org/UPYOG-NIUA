import json
from unittest.mock import MagicMock, patch

import cronJobAPIConfig as cfg


def test_get_user_when_no_users_raises_and_returns_none():
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {"user": []}

    with patch.dict(
        "os.environ",
        {
            "USER_SERVICE_HOST": "http://user",
            "USER_SEARCH": "/user/_search",
            "TENANT_ID": "pg.citya",
            "USER_NAME": "system-user",
        },
    ), patch("cronJobAPIConfig.requests.request", return_value=mock_response):
        assert cfg.getUser() is None


def test_trigger_cronjob_posts_request_info_to_pqm_scheduler():
    mock_response = MagicMock()
    mock_response.status_code = 200

    request_info = {"apiId": "Rainmaker", "userInfo": {"uuid": "u1"}}

    with patch.dict(
        "os.environ",
        {"PQM_SERVICE_HOST": "http://pqm", "PQM_SCHEDULER": "/pqm/scheduler/_trigger"},
    ), patch("cronJobAPIConfig.requests.request", return_value=mock_response) as mock_req:
        cfg.triggerCronjob(request_info)

    mock_req.assert_called_once()
    args, kwargs = mock_req.call_args
    assert args[0] == "POST"
    assert args[1] == "http://pqm/pqm/scheduler/_trigger"
    body = json.loads(kwargs["data"])
    assert body["RequestInfo"]["userInfo"]["uuid"] == "u1"

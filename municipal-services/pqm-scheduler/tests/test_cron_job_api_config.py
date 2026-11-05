import json
import sys
from unittest.mock import MagicMock

sys.modules.setdefault("requests", MagicMock())

import cronJobAPIConfig as cfg  # noqa: E402


def test_get_request_info_merges_user_info():
    user = {"uuid": "u1", "tenantId": "pg.citya"}
    info = cfg.getRequestInfo(user)
    assert info["userInfo"] == user
    assert info["apiId"] == "Rainmaker"


def test_get_request_info_is_json_serializable():
    info = cfg.getRequestInfo({"id": 1})
    json.dumps(info)

import os
import io
import time
import yaml
import json
import base64
import logging
import requests
from datetime import datetime, timedelta
from typing import Dict, Any, List, Tuple, Optional
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

llm = None
if os.environ.get("GROQ_API_KEY"):
    try:
        from langchain_groq import ChatGroq
        llm = ChatGroq(model=os.environ.get("GROQ_MODEL", "openai/gpt-oss-20b"), temperature=0)
    except ImportError:
        pass


SENSITIVE_LOG_KEYS = {
    "authtoken", "auth_token", "password", "access_token", "token",
    "otp", "authorization", "basic_auth", "secret", "api_key", "jwt",
    "refreshtoken", "refresh_token", "userpassword"
}


def sanitize_payload_for_logging(data: Any) -> Any:
    """Recursively redacts sensitive keys like authToken, password, access_token, otp before logging."""
    if isinstance(data, dict):
        sanitized = {}
        for k, v in data.items():
            k_lower = str(k).lower().replace("-", "").replace("_", "")
            if any(sens in k_lower for sens in ["authtoken", "password", "accesstoken", "secret", "apikey", "otp", "jwt"]):
                sanitized[k] = "[REDACTED]"
            else:
                sanitized[k] = sanitize_payload_for_logging(v)
        return sanitized
    elif isinstance(data, list):
        return [sanitize_payload_for_logging(item) for item in data]
    return data


# ==============================================================
# CONFIG LOADER — reads config.yml once at startup
# ==============================================================

def _load_config() -> dict:
    """Load API configuration from config.yml sitting in project root."""
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    config_path = os.path.join(base_dir, "config.yml")
    try:
        with open(config_path, "r") as f:
            cfg = yaml.safe_load(f)
        logger.info(f"Config loaded from {config_path}")
        return cfg.get("upyog", {})
    except Exception as e:
        logger.error(f"Failed to load config.yml: {e}. All API calls will fail.")
        return {}


_cfg = _load_config()

# Shortcuts used across the client
UPYOG_BASE_URL = _cfg.get("base_url", "")
MDMS_TENANT_ID = _cfg.get("tenant_id", "")
_ep  = _cfg.get("endpoints", {})          # API endpoint paths
_bd  = _cfg.get("booking_defaults", {})    # booking time/status/address defaults
_dt  = _cfg.get("document_types", {})     # document type strings
_sd  = _cfg.get("search_defaults", {})    # search limits, sort orders
_fs  = _cfg.get("filestore", {})          # filestore tenant/module
_mu  = _cfg.get("master_user", {})        # fallback system user profile
_mc  = _cfg.get("mdms", {})               # MDMS module/master names

# Secrets — from .env only
_BASIC_AUTH = os.environ.get("UPYOG_BASIC_AUTH", "Basic ZWdvdjZ1c2VyOmVnb3Y2dXNlcjFzZWNyZXQ=")
_USERNAME   = os.environ.get("UPYOG_USERNAME", "9999999999")
_PASSWORD   = os.environ.get("UPYOG_PASSWORD", "123456")

# Global MDMS cache to avoid repeated HTTP calls for the same module
_mdms_cache: dict = {}


# ==============================================================
# UPYOG API CLIENT CLASS
# ==============================================================

class UpyogAPI:
    """
    Handles OAuth token lifecycle and all HTTP POST calls to UPYOG.
    Credentials and endpoint paths come entirely from config.yml / .env.
    """

    def __init__(self):
        self.auth_token = None
        self.token_expiry = 0

    def _fetch_live_token(self):
        """Fetches a fresh OAuth token from UPYOG."""
        auth_cfg   = _cfg.get("auth", {})
        token_path = auth_cfg.get("token_path")
        url = f"{UPYOG_BASE_URL}{token_path}?_={int(time.time() * 1000)}"

        headers = {
            "Authorization": _BASIC_AUTH,
            "Content-Type":  "application/x-www-form-urlencoded"
        }
        data = (
            f"username={_USERNAME}"
            f"&password={_PASSWORD}"
            f"&tenantId={auth_cfg.get('tenant_id')}"
            f"&userType={auth_cfg.get('user_type')}"
            f"&scope={auth_cfg.get('scope')}"
            f"&grant_type={auth_cfg.get('grant_type')}"
        )
        logger.info("Fetching live OAuth token from UPYOG...")
        res = requests.post(url, headers=headers, data=data)
        res.raise_for_status()
        token_data = res.json()
        self.auth_token    = token_data.get("access_token")
        buffer             = auth_cfg.get("refresh_buffer_seconds")
        self.token_expiry  = time.time() + token_data.get("expires_in") - buffer
        logger.info("OAuth token fetched successfully.")

    def get_live_token(self) -> str:
        """Returns a valid token, refreshing if expired."""
        if not self.auth_token or time.time() > self.token_expiry:
            self._fetch_live_token()
        return self.auth_token

    def get_request_info(self, phone_anchor: str = None) -> Dict[str, Any]:
        """Builds the UPYOG RequestInfo block for authenticated citizen sessions."""
        user_info  = None
        auth_token = None

        if phone_anchor and phone_anchor != "default":
            try:
                from services.user_service import get_user_profile_info
                user_info = get_user_profile_info(phone_anchor)
                if user_info:
                    auth_token = user_info.get("_auth_token")
            except Exception:
                try:
                    from app import get_user_profile_info
                    user_info = get_user_profile_info(phone_anchor)
                    if user_info:
                        auth_token = user_info.get("_auth_token")
                except Exception:
                    pass

        # Fallback 1: check in-memory profile cache
        if not user_info or not auth_token:
            try:
                from services.user_service import _USER_PROFILE_CACHE
                for p_num, p_info in _USER_PROFILE_CACHE.items():
                    if isinstance(p_info, dict) and p_info.get("_auth_token"):
                        user_info = p_info
                        auth_token = p_info.get("_auth_token")
                        break
            except Exception:
                try:
                    from app import _USER_PROFILE_CACHE
                    for p_num, p_info in _USER_PROFILE_CACHE.items():
                        if isinstance(p_info, dict) and p_info.get("_auth_token"):
                            user_info = p_info
                            auth_token = p_info.get("_auth_token")
                            break
                except Exception:
                    pass

        # Fallback 2: scan Redis/memory for any authenticated citizen profile
        if not user_info or not auth_token:
            try:
                from database import r_client
                for k in r_client.scan_iter("user_profile_info:*"):
                    raw = r_client.get(k)
                    if raw:
                        parsed = json.loads(raw.decode('utf-8') if isinstance(raw, bytes) else raw)
                        if parsed and parsed.get("_auth_token"):
                            user_info = parsed
                            auth_token = parsed.get("_auth_token")
                            break
            except Exception as e:
                logger.error(f"[get_request_info] Error scanning active profiles: {e}")

        if user_info and auth_token:
            return {
                "apiId":              "Rainmaker",
                "authToken":          auth_token,
                "userInfo":           user_info,
                "msgId":              f"{int(time.time() * 1000)}|en_IN",
                "plainAccessRequest": {}
            }

        raise PermissionError(
            f"No authenticated session found for phone '{phone_anchor}'. "
            "User must log in with their registered UPYOG mobile number first."
        )

    def get_system_request_info(self) -> Dict[str, Any]:
        """Returns a system-level RequestInfo using the master OAuth token."""
        token = None
        try:
            token = self.get_live_token()
        except Exception as e:
            logger.warning(f"Could not fetch live system OAuth token: {e}. Proceeding without token for system call.")

        return {
            "apiId":      "Rainmaker",
            "authToken":  token,
            "userInfo": {
                "id":            _mu.get("id", 0),
                "uuid":          _mu.get("uuid", ""),
                "userName":      _mu.get("user_name", ""),
                "name":          _mu.get("name", ""),
                "mobileNumber":  _mu.get("mobile_number", ""),
                "emailId":       _mu.get("email", ""),
                "locale":        _mu.get("locale", "en_IN"),
                "type":          _mu.get("type", "CITIZEN"),
                "roles": [{
                    "name":     _mu.get("role_name", "Citizen"),
                    "code":     _mu.get("role_code", "CITIZEN"),
                    "tenantId": _mu.get("tenant_id", "pg")
                }],
                "active":        True,
                "tenantId":      _mu.get("tenant_id", "pg"),
                "permanentCity": None
            },
            "msgId":              f"{int(time.time() * 1000)}|en_IN",
            "plainAccessRequest": {}
        }

    def post(self, url: str, payload: dict) -> dict:
        """POST with automatic token injection and 401 retry."""
        is_system = False
        req_info = payload.get("RequestInfo", {})
        if "authToken" not in req_info or req_info.get("authToken") is None:
            try:
                payload["RequestInfo"]["authToken"] = self.get_live_token()
                is_system = True
            except Exception as e:
                logger.warning(f"[UpyogAPI.post] Could not fetch live system OAuth token: {e}. Proceeding with payload as is.")
            
        headers = {"Content-Type": "application/json"}
        start_t = time.time()
        logger.info(f"[UpyogAPI.post] POST {url} | systemToken={is_system} | payloadKeys={list(payload.keys())}")
        res = requests.post(url, json=payload, headers=headers)
        elapsed = time.time() - start_t
        logger.info(f"[UpyogAPI.post] Response HTTP {res.status_code} from {url} in {elapsed:.2f}s")
        
        if res.status_code == 401 and is_system:
            logger.info("[UpyogAPI.post] Got 401 — refreshing token and retrying...")
            self.auth_token = None
            try:
                payload["RequestInfo"]["authToken"] = self.get_live_token()
                res = requests.post(url, json=payload, headers=headers)
                logger.info(f"[UpyogAPI.post] Retry Response HTTP {res.status_code} from {url}")
            except Exception as e:
                logger.warning(f"[UpyogAPI.post] Token refresh failed: {e}")
            
        if res.status_code >= 400:
            try:
                error_body = res.json()
                logger.error(f"[UpyogAPI.post] UPYOG {res.status_code} Error details: {json.dumps(sanitize_payload_for_logging(error_body), indent=2)}")
            except Exception:
                logger.error(f"[UpyogAPI.post] UPYOG {res.status_code} Error raw text: {res.text[:500]}")
                
        res.raise_for_status()
        return res.json()


api_client = UpyogAPI()


# ==============================================================
# AUTH & OTP APIS
# ==============================================================

def send_otp_upyog(mobile: str) -> dict:
    """Sends OTP to the given mobile number via UPYOG OTP API."""
    auth_cfg = _cfg.get("auth", {})
    state_tenant = _cfg.get("state_tenant", "pg")
    url = f"{UPYOG_BASE_URL}{_cfg.get('endpoints', {}).get('send_otp', '/user-otp/v1/_send')}?tenantId={state_tenant}&_={int(time.time() * 1000)}"
    payload = {
        "otp": {
            "mobileNumber": mobile,
            "tenantId": state_tenant,
            "userType": auth_cfg.get("user_type", "CITIZEN").lower(),
            "type": "login"
        },
        "RequestInfo": {
            "apiId": "Rainmaker",
            "msgId": f"{int(time.time() * 1000)}|en_IN",
            "plainAccessRequest": {}
        }
    }
    headers = {"Content-Type": "application/json"}
    if _BASIC_AUTH:
        headers["Authorization"] = _BASIC_AUTH
    try:
        res = requests.post(url, json=payload, headers=headers)
        if res.status_code not in (200, 201):
            logger.error(f"UPYOG OTP Error {res.status_code}: {res.text}")
        else:
            logger.info(f"UPYOG OTP sent successfully (HTTP {res.status_code})")
        
        try:
            return res.json()
        except ValueError:
            logger.error(f"UPYOG OTP Non-JSON response: {res.text}")
            return {"error": f"Invalid response from server: {res.status_code}", "details": res.text[:200]}
            
    except Exception as e:
        logger.error(f"Error sending UPYOG OTP: {e}")
        return {"error": str(e)}


def verify_otp_upyog(mobile: str, otp: str) -> dict:
    """Verifies OTP for the given mobile number via UPYOG OAuth token endpoint."""
    auth_cfg = _cfg.get("auth", {})
    state_tenant = _cfg.get("state_tenant", "pg")
    url = f"{UPYOG_BASE_URL}{auth_cfg.get('token_path', '/user/oauth/token')}"
    data = {
        "username": mobile,
        "password": otp,
        "grant_type": auth_cfg.get("grant_type", "password"),
        "scope": auth_cfg.get("scope", "read"),
        "tenantId": state_tenant,
        "userType": auth_cfg.get("user_type", "CITIZEN")
    }
    headers = {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": _BASIC_AUTH
    }
    try:
        res = requests.post(url, data=data, headers=headers)
        if res.status_code != 200:
            logger.error(f"UPYOG Verify OTP Error {res.status_code}: {res.text}")
        try:
            return res.json()
        except ValueError:
            logger.error(f"UPYOG Verify OTP Non-JSON response: {res.text}")
            return {"error": f"Invalid response from server: {res.status_code}", "details": res.text[:200]}
    except Exception as e:
        logger.error(f"Error verifying UPYOG OTP: {e}")
        return {"error": str(e)}


def fetch_user_details_upyog(mobile: str, auth_token: str) -> dict:
    """Fetches user profile details from UPYOG /user/_search."""
    state_tenant = _cfg.get("state_tenant", "pg")
    url = f"{UPYOG_BASE_URL}{_cfg.get('endpoints', {}).get('user_search', '/user/_search')}?_={int(time.time() * 1000)}"
    payload = {
        "tenantId": state_tenant,
        "userName": mobile,
        "pageSize": "100",
        "RequestInfo": {
            "apiId": "Rainmaker",
            "authToken": auth_token,
            "msgId": f"{int(time.time() * 1000)}|en_IN",
            "plainAccessRequest": {}
        }
    }
    try:
        res = requests.post(url, json=payload, headers={"Content-Type": "application/json"})
        if res.status_code != 200:
            logger.error(f"UPYOG User Search Error {res.status_code}: {res.text}")
        try:
            data = res.json()
        except ValueError:
            logger.error(f"UPYOG User Search Non-JSON response: {res.text}")
            return {}
            
        if "user" in data and len(data["user"]) > 0:
            return data["user"][0]
        return {}
    except Exception as e:
        logger.error(f"Error fetching user details from UPYOG: {e}")
        return {}


def verify_user_auth(auth_token: str, uuid_or_mobile: Optional[str] = None, tenant_id: Optional[str] = None) -> Tuple[bool, dict]:
    """Validates an auth token with UPYOG /user/_search and returns (is_valid, user_info)."""
    state_tenant = tenant_id or _cfg.get("state_tenant", "pg")
    url = f"{UPYOG_BASE_URL}{_cfg.get('endpoints', {}).get('user_search', '/user/_search')}?_={int(time.time() * 1000)}"
    headers = {
        "Content-Type": "application/json",
        "auth-token": str(auth_token)
    }
    payload = {
        "tenantId": state_tenant,
        "pageSize": "100",
        "RequestInfo": {
            "apiId": "Rainmaker",
            "authToken": auth_token,
            "msgId": f"{int(time.time() * 1000)}|en_IN",
            "plainAccessRequest": {}
        }
    }
    if uuid_or_mobile and uuid_or_mobile != "default":
        if "-" in str(uuid_or_mobile):
            payload["uuid"] = [str(uuid_or_mobile)]
        elif str(uuid_or_mobile).isdigit():
            payload["userName"] = str(uuid_or_mobile)

    max_retries = 2
    timeout_seconds = 5
    
    for attempt in range(max_retries + 1):
        try:
            logger.info(f"[Auth] Verifying token for {uuid_or_mobile} (attempt {attempt + 1}/{max_retries + 1})...")
            res = requests.post(url, json=payload, headers=headers, timeout=timeout_seconds)
            
            if res.status_code in [500, 502, 504]:
                logger.warning(f"[Auth] Received transient status {res.status_code} from UPYOG user search. Retrying...")
                if attempt < max_retries:
                    time.sleep(1)
                    continue
                
            if res.status_code != 200:
                logger.error(f"UPYOG Verify Auth Error {res.status_code}: {res.text}")
                return False, {}
                
            try:
                data = res.json()
            except ValueError:
                logger.error(f"UPYOG Verify Auth Non-JSON response: {res.text}")
                return False, {}
                
            if "user" in data and len(data["user"]) > 0:
                logger.info(f"[Auth] Verified user successfully: {data['user'][0].get('userName') or data['user'][0].get('mobileNumber')}")
                return True, data["user"][0]
            else:
                logger.error(f"UPYOG Verify Auth failed - No user found for {uuid_or_mobile}. Response: {data}")
                return False, {}
                
        except (requests.exceptions.Timeout, requests.exceptions.ConnectionError) as net_err:
            logger.warning(f"[Auth] Network error/timeout on attempt {attempt + 1}: {net_err}")
            if attempt < max_retries:
                time.sleep(1)
                continue
            logger.error("[Auth] All auth verification retries failed due to network errors.")
            return False, {}
        except Exception as e:
            logger.error(f"Error checking user auth: {e}")
            return False, {}
            
    return False, {}


# ==============================================================
# FILESTORE API
# ==============================================================

def upload_to_filestore(file_name: str, file_data_base64: str, token: str) -> str:
    """Uploads a document to UPYOG's filestore and returns its unique file ID."""
    logger.info(f"[upload_to_filestore] Uploading file '{file_name}' (payload length: {len(file_data_base64) if file_data_base64 else 0} chars)")
    try:
        if "," in file_data_base64:
            header, encoded = file_data_base64.split(",", 1)
            mime_type = header.split(":")[1].split(";")[0]
        else:
            encoded   = file_data_base64
            mime_type = "application/pdf"

        file_bytes = base64.b64decode(encoded)
        url = f"{UPYOG_BASE_URL}{_ep.get('filestore_upload')}"
        logger.info(f"[upload_to_filestore] Sending {len(file_bytes)} bytes ({mime_type}) to {url}")
        resp = requests.post(
            url,
            headers={"auth-token": token},
            files={"file": (file_name, io.BytesIO(file_bytes), mime_type)},
            data={"tenantId": _fs.get("tenant", "pg"), "module": _fs.get("module")}
        )
        logger.info(f"[upload_to_filestore] HTTP {resp.status_code} received")
        resp_json = resp.json()
        if "files" in resp_json and resp_json["files"]:
            file_store_id = resp_json["files"][0].get("fileStoreId", "")
            logger.info(f"[upload_to_filestore] Successfully uploaded! fileStoreId: {file_store_id}")
            return file_store_id
        logger.warning(f"[upload_to_filestore] No fileStoreId returned in response: {resp_json}")
    except Exception as e:
        logger.error(f"[upload_to_filestore] Filestore upload exception: {e}")
    return ""


# ==============================================================
# ADVERTISEMENT BOOKING APIS & MDMS
# ==============================================================

def search_ads(mobile_number: str, booking_no: str = None, status: str = None, latest: bool = False) -> str:
    """Searches UPYOG database for past advertisement bookings using the citizen's mobile number."""
    logger.info(f"[search_ads] Called with mobile_number={mobile_number}, booking_no={booking_no}, status={status}, latest={latest}")
    url = (
        f"{UPYOG_BASE_URL}{_ep.get('search_ads')}"
        f"?tenantId={MDMS_TENANT_ID}"
        f"&limit={_sd.get('ads_limit')}"
        f"&sortOrder={_sd.get('ads_sort_order')}"
        f"&sortBy={_sd.get('ads_sort_by')}"
        f"&offset={_sd.get('ads_offset')}"
        f"&mobileNumber={mobile_number}"
    )
    if booking_no:
        url += f"&bookingNo={booking_no}"
    if status:
        url += f"&status={status}"

    payload = {"RequestInfo": api_client.get_request_info(mobile_number)}
    try:
        data = api_client.post(url, payload)
        bookings = data.get("bookingApplication", [])
        logger.info(f"[search_ads] Successfully retrieved {len(bookings)} bookings for {mobile_number}")
        if latest and bookings:
            bookings = [bookings[0]]

        optimized = []
        for b in bookings:
            optimized.append({
                "bookingNo":     b.get("bookingNo"),
                "applicationNo": b.get("applicationNo"),
                "bookingDate":   (b.get("cartDetails") or [{}])[0].get("bookingDate"),
                "status":        b.get("bookingStatus"),
                "applicantDetail": {
                    "applicantName":     b.get("applicantDetail", {}).get("applicantName"),
                    "applicantMobileNo": b.get("applicantDetail", {}).get("applicantMobileNo")
                },
                "address": {
                    "houseNo":    b.get("address", {}).get("houseNo"),
                    "streetName": b.get("address", {}).get("streetName"),
                    "city":       b.get("address", {}).get("city")
                },
                "documents": [
                    {"documentType": d.get("documentType")}
                    for d in b.get("documents", [])
                ]
            })
        return json.dumps(optimized)
    except Exception as e:
        logger.error(f"[search_ads] Error fetching bookings: {e}")
        return json.dumps({"error": str(e), "message": "Failed to fetch bookings from UPYOG"})


def mdms_get(module_name: str, master_name: str) -> list:
    """Fetches dropdown choices like AdType, FaceArea, or Location live from UPYOG MDMS API."""
    logger.info(f"[mdms_get] Requested master={master_name} for module={module_name}")
    if master_name == "NightLight":
        return ["Yes", "No"]

    if module_name in _mdms_cache:
        items = _mdms_cache[module_name].get(master_name, [])
        logger.info(f"[mdms_get] Cache HIT for {module_name}/{master_name} ({len(items)} items)")
        return [(item.get("name") or item.get("code") or "").strip() for item in items if item.get("active", True) and (item.get("name") or item.get("code"))]

    state_tenant = MDMS_TENANT_ID.split(".")[0]

    adv_module = _mc.get("advertisement_module")
    master_details = [{"name": master_name}]
    if module_name == adv_module:
        master_details = [
            {"name": m} for m in _mc.get("advertisement_masters")
        ]

    url = f"{UPYOG_BASE_URL}{_ep.get('mdms_search')}?tenantId={state_tenant}"
    payload = {
        "MdmsCriteria": {
            "tenantId": state_tenant,
            "moduleDetails": [{
                "moduleName":  module_name,
                "masterDetails": master_details
            }]
        },
        "RequestInfo": api_client.get_system_request_info()
    }
    try:
        data        = api_client.post(url, payload)
        module_data = data.get("MdmsRes", {}).get(module_name, {})
        _mdms_cache[module_name] = module_data
        items = module_data.get(master_name, [])
        options = [(item.get("name") or item.get("code") or "").strip() for item in items if item.get("active", True) and (item.get("name") or item.get("code"))]
        logger.info(f"[mdms_get] Fetched {len(options)} options from MDMS for {module_name}/{master_name}")
        return options
    except Exception as e:
        logger.error(f"[mdms_get] MDMS error for {module_name}/{master_name}: {e}")
        return []


def slot_search(addType: str, faceArea: str, location: str,
                start_date: str, end_date: str, nightLight: bool,
                phone_anchor: str = None) -> str:
    """Checks UPYOG server to find available advertisement slots for a given date and location."""
    logger.info(f"[slot_search] Query: addType='{addType}', faceArea='{faceArea}', location='{location}', start='{start_date}', end='{end_date}', nightLight='{nightLight}', phone='{phone_anchor}'")
    url = f"{UPYOG_BASE_URL}{_ep.get('slot_search')}"
    req_info = None
    if phone_anchor:
        try:
            req_info = api_client.get_request_info(phone_anchor)
        except Exception:
            pass
    if not req_info:
        req_info = api_client.get_system_request_info()

    payload = {
        "RequestInfo": req_info,
        "advertisementSlotSearchCriteria": [{
            "addType":          addType,
            "faceArea":         faceArea,
            "location":         location,
            "bookingStartDate": start_date,
            "bookingEndDate":   end_date,
            "nightLight":       str(nightLight).lower() == "true" or nightLight is True or str(nightLight).lower() == "yes",
            "isTimerRequired":  False,
            "tenantId":         MDMS_TENANT_ID
        }]
    }
    try:
        data  = api_client.post(url, payload)
        slots = data.get("advertisementSlotAvailabiltityDetails", [])
        today_str = datetime.now().strftime("%Y-%m-%d")
        if slots:
            normalized = []
            for i, s in enumerate(slots):
                d = s.get("bookingStartDate")
                if not d:
                    try:
                        sd = datetime.strptime(start_date, "%Y-%m-%d")
                        d  = (sd + timedelta(days=i)).strftime("%Y-%m-%d")
                    except Exception:
                        d = start_date
                
                if not d or str(d).strip() <= today_str:
                    continue

                normalized.append({
                    "type":   s.get("addType")       or addType,
                    "area":   s.get("faceArea")       or faceArea,
                    "light":  "Yes" if s.get("nightLight", nightLight) else "No",
                    "date":   d,
                    "status": s.get("status") or s.get("bookingStatus") or "AVAILABLE"
                })
            logger.info(f"[slot_search] Returned {len(normalized)} normalized available slots (out of {len(slots)} raw)")
            return json.dumps(normalized)
        logger.warning(f"[slot_search] No available slots returned by UPYOG for {addType}/{location}")
        return json.dumps([])
    except Exception as e:
        logger.error(f"slot_search error from UPYOG API: {e}")
        return json.dumps([])


def fetch_bill(booking_no: str, mobile_number: str = None) -> str:
    """Calculates and fetches the estimated bill amount for an advertisement booking."""
    logger.info(f"[fetch_bill] Called for booking_no='{booking_no}', mobile='{mobile_number}'")
    try:
        request_info = api_client.get_request_info(mobile_number)
    except PermissionError as auth_err:
        logger.warning(f"[fetch_bill] Auth permission error: {auth_err}")
        return f"Authentication required: {auth_err}"
    
    url = (
        f"{UPYOG_BASE_URL}{_ep.get('fetch_bill')}"
        f"?tenantId={MDMS_TENANT_ID}"
        f"&consumerCode={booking_no}"
        f"&businessService={_sd.get('bill_business_service')}"
    )
    payload = {"RequestInfo": request_info}
    try:
        data  = api_client.post(url, payload)
        bills = data.get("Bill", [])
        if bills:
            amount = bills[0].get("totalAmount")
            logger.info(f"[fetch_bill] Successfully fetched bill for {booking_no}: ₹{amount}")
            return f"Total Estimated Amount: ₹{amount}"
        logger.warning(f"[fetch_bill] No bill records returned for booking {booking_no}")
        return "Could not find a bill for this booking."
    except Exception as e:
        logger.error(f"[fetch_bill] Error fetching bill for {booking_no}: {e}")
        return "Unable to fetch the bill details at this moment due to a technical issue. Please try again later."


def create_booking(booking_details_json: str) -> str:
    """Submits all collected booking data to UPYOG server to officially register the advertisement booking."""
    try:
        details = json.loads(booking_details_json)
    except Exception:
        return "Error: Could not parse booking details. Please provide valid JSON."

    address_str = details.get("address", "")
    addr_details = {}
    if address_str and llm:
        try:
            from langchain_core.messages import SystemMessage
            prompt = f"""You are a strict data parser for UPYOG addresses.
Given this full address: "{address_str}"
Extract these fields as a JSON object:
- "pincode": 6-digit postal code (e.g. "110001", "180091")
- "city": city name
- "locality": locality name
- "streetName": street name
- "houseNo": house number or building number (e.g. "E-56", "23")
- "landmark": landmark if present, else null

Reply with ONLY the valid JSON object (no markdown, no other text)."""
            res = llm.invoke([SystemMessage(content=prompt)])
            import re
            m = re.search(r'\{.*\}', res.content.strip(), re.DOTALL)
            if m:
                addr_details = json.loads(m.group(0))
                logger.info(f"[Auth] Extracted address details via LLM: {addr_details}")
        except Exception as e:
            logger.error(f"Error parsing address via LLM: {e}")

    mobile_number = details.get("mobileNumber", "")
    try:
        citizen_request_info = api_client.get_request_info(mobile_number)
    except PermissionError:
        try:
            citizen_request_info = api_client.get_request_info()
        except PermissionError:
            return (
                "You must be logged in with your registered UPYOG mobile number to create a booking. "
                "Please log in first using the login button."
            )

    url = f"{UPYOG_BASE_URL}{_ep.get('create_booking')}"

    selected_slots = details.get("selected_slots", [])
    if not selected_slots:
        selected_slots = [{"date": details.get("start_date", "")}]

    cart_details = []
    for slot in selected_slots:
        cart_details.append({
            "addType":         slot.get("type",  details.get("addType", "")),
            "faceArea":        slot.get("area",  details.get("faceArea", "")),
            "location":        details.get("location", ""),
            "nightLight":      str(slot.get("light", details.get("nightLight", "No"))).lower() in ["yes", "true"],
            "bookingDate":     slot.get("date",  details.get("start_date", "")),
            "bookingFromTime": _bd.get("booking_from_time"),
            "bookingToTime":   _bd.get("booking_to_time"),
            "status":          _bd.get("booking_status")
        })

    documents = []
    seen_ids = set()

    def _get_unique_file_id(fid: str) -> str:
        if not fid or fid.endswith("_doc") or len(fid) < 10:
            return ""
        if fid not in seen_ids:
            seen_ids.add(fid)
            return fid
        try:
            token = citizen_request_info.get("authToken")
            url_api = f"{UPYOG_BASE_URL}/filestore/v1/files/id?tenantId={_fs.get('tenant', 'pg')}&fileStoreId={fid}"
            file_resp = requests.get(url_api, headers={"auth-token": token})
            
            if file_resp.status_code == 200:
                upload_url = f"{UPYOG_BASE_URL}{_ep.get('filestore_upload')}"
                up_resp = requests.post(
                    upload_url,
                    headers={"auth-token": token},
                    files={"file": (f"{fid}_copy.pdf", io.BytesIO(file_resp.content), "application/pdf")},
                    data={"tenantId": _fs.get("tenant", "pg"), "module": _fs.get("module")}
                )
                if up_resp.status_code in [200, 201]:
                    new_id = up_resp.json()["files"][0]["fileStoreId"]
                    seen_ids.add(new_id)
                    return new_id
                else:
                    logger.warning(f"Failed to upload copy: {up_resp.status_code} - {up_resp.text}")
            else:
                logger.warning(f"Failed to download original file: {file_resp.status_code} - {file_resp.text}")
        except Exception as e:
            logger.error(f"Failed to copy file for deduplication: {e}")
            
        return ""

    doc_sample_val = _get_unique_file_id(details.get("doc_sample", ""))
    if doc_sample_val:
        documents.append({
            "documentType": _dt.get("sample"),
            "fileStoreId":  doc_sample_val,
            "documentUid":  doc_sample_val
        })

    doc_address_val = _get_unique_file_id(details.get("doc_address", ""))
    if doc_address_val:
        documents.append({
            "documentType": _dt.get("address"),
            "fileStoreId":  doc_address_val,
            "documentUid":  doc_address_val
        })

    doc_identity_val = _get_unique_file_id(details.get("doc_identity", ""))
    if doc_identity_val:
        documents.append({
            "documentType": _dt.get("identity"),
            "fileStoreId":  doc_identity_val,
            "documentUid":  doc_identity_val
        })

    payload = {
        "RequestInfo": citizen_request_info,
        "bookingApplication": {
            "tenantId": MDMS_TENANT_ID,
            "applicantDetail": {
                "applicantName":              details.get("applicantName", "Unknown"),
                "applicantMobileNo":           details.get("mobileNumber", ""),
                "applicantAlternateMobileNo":  "",
                "applicantEmailId":            details.get("emailId", "")
            },
            "address": {
                "pincode":      addr_details.get("pincode") or details.get("pincode") or _bd.get("default_pincode"),
                "city":         addr_details.get("city") or details.get("city") or _bd.get("default_city"),
                "cityCode":     details.get("cityCode", _bd.get("default_city_code")),
                "locality":     addr_details.get("locality") or details.get("locality") or _bd.get("default_locality"),
                "localityCode": details.get("localityCode", _bd.get("default_locality_code")),
                "streetName":   addr_details.get("streetName") or details.get("streetName") or _bd.get("default_street"),
                "addressLine1": address_str if address_str else f"{addr_details.get('houseNo', '')}, {addr_details.get('streetName', '')}, {addr_details.get('locality', '')}".strip(", "),
                "addressLine2": "",
                "houseNo":      addr_details.get("houseNo") or details.get("houseNo") or _bd.get("default_house_no"),
                "landmark":     addr_details.get("landmark") or details.get("landmark") or _bd.get("default_landmark")
            },
            "cartDetails":   cart_details,
            "bookingStatus": _bd.get("booking_status"),
            "documents":     documents,
            "workflow":      None
        }
    }
    try:
        logger.info(f"=== CREATE BOOKING PAYLOAD ===\n{json.dumps(sanitize_payload_for_logging(payload), indent=2)}\n==============================")
        data = api_client.post(url, payload)
        logger.info(f"=== CREATE BOOKING RESPONSE ===\n{json.dumps(sanitize_payload_for_logging(data), indent=2)}\n===============================")
        app_no = (data.get("bookingApplication") or [{}])[0].get("bookingNo")
        if app_no:
            return f"Booking successfully created! Application Number: {app_no}"
        return f"Booking creation succeeded but no application number was returned. Raw: {data}"
    except Exception as e:
        logger.error(f"[create_booking] Error creating booking: {e}", exc_info=True)
        return (
            "I apologize, but we could not complete your advertisement booking due to a technical issue with the municipal portal. "
            "Please verify your booking details and try again, or contact the UPYOG support desk."
        )


# ==============================================================
# GRIEVANCE (PGR) APIS
# ==============================================================

def pgr_get_categories(phone_anchor: str = None) -> dict:
    """Fetches the list of all grievance complaint categories from the UPYOG server."""
    grv = _cfg.get("grievance", {})
    state_tenant = grv.get("state_tenant", "pg")
    module       = grv.get("mdms_module", "RAINMAKER-PGR")
    master       = grv.get("mdms_master",  "ServiceDefs")

    url = f"{UPYOG_BASE_URL}{_ep.get('pgr_mdms')}?tenantId={state_tenant}"
    payload = {
        "MdmsCriteria": {
            "tenantId": state_tenant,
            "moduleDetails": [{
                "moduleName": module,
                "masterDetails": [{"name": master}]
            }]
        },
        "RequestInfo": api_client.get_system_request_info()
    }
    try:
        data = api_client.post(url, payload)
        defs = data.get("MdmsRes", {}).get(module, {}).get(master, [])
        structured: dict = {}
        for d in defs:
            if not d.get("active", True):
                continue
            menu = d.get("menuPath") or "Others"
            structured.setdefault(menu, [])
            structured[menu].append({
                "name": d.get("name", ""),
                "code": d.get("serviceCode", "")
            })
        logger.info(f"[pgr_get_categories] Fetched {sum(len(v) for v in structured.values())} items across {len(structured)} groups")
        return structured
    except Exception as e:
        logger.error(f"pgr_get_categories error: {e}")
        return {}


def pgr_get_localities(phone_anchor: str = None) -> list:
    """Fetches the list of all localities/areas for the city from the UPYOG server."""
    grv       = _cfg.get("grievance", {})
    tenant_id = grv.get("tenant_id",      "pg.citya")
    hierarchy = grv.get("hierarchy_type", "ADMIN")
    boundary  = grv.get("boundary_type",  "Locality")

    url = (
        f"{UPYOG_BASE_URL}{_ep.get('pgr_locality')}"
        f"?hierarchyTypeCode={hierarchy}"
        f"&boundaryType={boundary}"
        f"&tenantId={tenant_id}"
    )
    payload = {"RequestInfo": api_client.get_system_request_info()}
    try:
        data       = api_client.post(url, payload)
        boundaries = data.get("TenantBoundary", [])
        if boundaries and boundaries[0].get("boundary"):
            locs = [
                {"name": b["name"], "code": b["code"]}
                for b in boundaries[0]["boundary"]
                if b.get("name") and b.get("code")
            ]
            logger.info(f"[pgr_get_localities] Fetched {len(locs)} localities")
            return locs
        return []
    except Exception as e:
        logger.error(f"pgr_get_localities error: {e}")
        return []


def pgr_create_complaint(complaint_json: str) -> str:
    """Submits all collected grievance data to UPYOG server to officially register the complaint."""
    try:
        details = json.loads(complaint_json)
    except Exception:
        return "Error: Could not parse complaint details. Please provide valid JSON."

    grv       = _cfg.get("grievance", {})
    tenant_id = grv.get("tenant_id",    "pg.citya")
    priority  = grv.get("priority",     "HIGH")
    source    = grv.get("source",       "web")

    phone_anchor = details.get("phone_number", "")

    try:
        request_info = api_client.get_request_info(phone_anchor)
        user_info    = request_info.get("userInfo", {})
    except PermissionError:
        try:
            request_info = api_client.get_request_info()
            user_info    = request_info.get("userInfo", {})
        except PermissionError:
            return (
                "You must be logged in with your registered UPYOG mobile number to register a complaint. "
                "Please log in first using the login button."
            )

    citizen_block = {
        "id":           user_info.get("id"),
        "userName":     user_info.get("userName") or phone_anchor,
        "name":         user_info.get("name", "Citizen"),
        "type":         user_info.get("type", "CITIZEN"),
        "mobileNumber": user_info.get("mobileNumber") or phone_anchor,
        "emailId":      user_info.get("emailId", ""),
        "roles":        user_info.get("roles", []),
        "tenantId":     user_info.get("tenantId", grv.get("state_tenant", "pg")),
        "uuid":         user_info.get("uuid"),
    }

    url = f"{UPYOG_BASE_URL}{_ep.get('pgr_create')}?tenantId={tenant_id}"
    payload = {
        "service": {
            "tenantId":        tenant_id,
            "serviceCode":     details.get("category_code", ""),
            "accountId":       user_info.get("uuid"),
            "citizen":         citizen_block,
            "priority":        priority,
            "description":     details.get("description", ""),
            "additionalDetail": {},
            "source":          source,
            "address": {
                "tenantId": tenant_id,
                "landmark": details.get("locality", ""),
                "city":     "City A",
                "district": "City A",
                "region":   "City A",
                "state":    "Demo",
                "pincode":  "143001",
                "locality": {
                    "code": details.get("locality_code", ""),
                    "name": details.get("locality", ""),
                },
                "geoLocation": {"latitude": 0.0, "longitude": 0.0},
            },
        },
        "workflow":    {"action": "APPLY", "comments": "", "assignes": []},
        "RequestInfo": request_info,
    }

    try:
        logger.info(f"=== PGR CREATE PAYLOAD ===\n{json.dumps(sanitize_payload_for_logging(payload), indent=2)}\n==========================")
        data = api_client.post(url, payload)
        logger.info(f"=== PGR CREATE RESPONSE ===\n{json.dumps(sanitize_payload_for_logging(data), indent=2)}\n===========================")

        if "Errors" in data:
            logger.error(f"[pgr_create_complaint] Portal error: {data.get('Errors')}")
            return (
                "**Submission Notice**\n\n"
                "We were unable to complete your complaint registration due to a temporary service issue. "
                "Please verify your details and try again shortly, or contact the UPYOG helpdesk."
            )

        sw_list = data.get("ServiceWrappers", [])
        if sw_list:
            ticket_id = sw_list[0].get("service", {}).get("serviceRequestId")
            if ticket_id:
                return (
                    "**Complaint Registered Successfully**\n\n"
                    f"- **Ticket Number:** `{ticket_id}`\n"
                    f"- **Status:** Submitted\n\n"
                )
        return (
            "**Complaint Submitted**\n\n"
            "Your complaint has been submitted. However, the server did not return a ticket number at this time. "
            "Please check your complaint status under **My Complaints** in the UPYOG portal."
        )

    except Exception as e:
        logger.error(f"pgr_create_complaint error: {e}", exc_info=True)
        return (
            "**Submission Notice**\n\n"
            "We were unable to complete your complaint registration due to a temporary technical issue. "
            "Please try again shortly, or contact the UPYOG helpdesk."
        )


def pgr_search_complaints_raw(phone_anchor: str = None, complaint_id: str = None) -> list:
    """Fetches raw JSON list of past complaints from UPYOG server for UI rendering."""
    try:
        phone_number = phone_anchor if (phone_anchor and phone_anchor != "default") else "default"
        try:
            req_info = api_client.get_request_info(phone_number)
        except Exception:
            req_info = api_client.get_request_info()

        user_info = req_info.get("userInfo", {})
        tenant_id = user_info.get("tenantId") or MDMS_TENANT_ID
        mobile = user_info.get("mobileNumber") or (phone_number if phone_number != "default" else "9999999999")
        
        url = f"{UPYOG_BASE_URL}{_ep.get('pgr_search', '/pgr-services/v2/request/_search')}?tenantId={tenant_id}"
        if complaint_id and ("PG-PGR" in complaint_id.upper() or "PGR" in complaint_id.upper()):
            url += f"&serviceRequestId={complaint_id.strip()}"
        else:
            url += f"&mobileNumber={mobile}"
        
        data = api_client.post(url, {"RequestInfo": req_info})
        sw_list = data.get("ServiceWrappers", [])

        if not sw_list and complaint_id:
            fallback_url = f"{UPYOG_BASE_URL}{_ep.get('pgr_search', '/pgr-services/v2/request/_search')}?tenantId={tenant_id}&mobileNumber={mobile}"
            data = api_client.post(fallback_url, {"RequestInfo": req_info})
            sw_list = data.get("ServiceWrappers", [])

        if not sw_list:
            return []

        results = []
        for sw in sw_list:
            svc = sw.get("service", {})
            req_id = svc.get("serviceRequestId", "N/A")
            service_code = svc.get("serviceCode", "N/A")
            status = svc.get("applicationStatus") or svc.get("status") or "Submitted"
            created_time = svc.get("auditDetails", {}).get("createdTime")
            filed_on = "N/A"
            if created_time:
                try:
                    from datetime import datetime
                    filed_on = datetime.fromtimestamp(created_time / 1000).strftime("%d %b %Y")
                except Exception:
                    pass
            addr = svc.get("address", {})
            loc_name = addr.get("locality", {}).get("name") or addr.get("city") or "N/A"
            
            if complaint_id:
                cid_clean = complaint_id.lower().strip()
                if cid_clean not in req_id.lower() and cid_clean not in service_code.lower():
                    continue

            results.append({
                "serviceRequestId": req_id,
                "bookingNo": req_id,
                "serviceCode": service_code,
                "status": status,
                "applicationStatus": status,
                "filed_on": filed_on,
                "bookingDate": filed_on,
                "locality": loc_name
            })

        return results[:4]
    except Exception as e:
        logger.error(f"[pgr_search_complaints_raw] error: {e}")
        return []


def pgr_search_complaints(phone_anchor: str = None, complaint_id: str = None) -> str:
    """Searches UPYOG server for past complaints linked to the user's phone number."""
    raw_list = pgr_search_complaints_raw(phone_anchor, complaint_id=complaint_id)
    if not raw_list:
        return "No previous complaints found for your account."
    return json.dumps(raw_list)


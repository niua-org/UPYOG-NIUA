"""Unit tests for services/validator.py."""

import unittest
from datetime import datetime, timedelta
from services.validator import is_empty, normalize, validate_date, validate_field, IST


class TestValidator(unittest.TestCase):
    """Test suite for validator.py pure functions."""

    def test_is_empty(self):
        self.assertTrue(is_empty(None))
        self.assertTrue(is_empty(""))
        self.assertTrue(is_empty("   "))
        self.assertTrue(is_empty("null"))
        self.assertTrue(is_empty("NULL"))
        self.assertTrue(is_empty("none"))
        self.assertTrue(is_empty("None"))

        self.assertFalse(is_empty("Rahul"))
        self.assertFalse(is_empty("0"))
        self.assertFalse(is_empty(123))

    def test_normalize(self):
        self.assertEqual(normalize("Hoarding Board"), "hoardingboard")
        self.assertEqual(normalize("Link_Colony"), "linkcolony")
        self.assertEqual(normalize("  Water   Supply_issue "), "watersupplyissue")

    def test_validate_date_format(self):
        is_valid, err, val = validate_date("start_date", "invalid-date", "From Date", {})
        self.assertFalse(is_valid)
        self.assertIn("YYYY-MM-DD", err)

    def test_validate_date_future_constraint(self):
        today = datetime.now(IST).date()
        past_date = (today - timedelta(days=1)).strftime("%Y-%m-%d")
        today_str = today.strftime("%Y-%m-%d")
        future_date = (today + timedelta(days=2)).strftime("%Y-%m-%d")

        # Past date must fail
        is_valid, err, _ = validate_date("start_date", past_date, "From Date", {})
        self.assertFalse(is_valid)
        self.assertIn("must be tomorrow or later", err)

        # Today date must fail (must be tomorrow or later)
        is_valid, err, _ = validate_date("start_date", today_str, "From Date", {})
        self.assertFalse(is_valid)

        # Tomorrow or later must pass
        is_valid, err, val = validate_date("start_date", future_date, "From Date", {})
        self.assertTrue(is_valid)
        self.assertIsNone(err)
        self.assertEqual(val, future_date)

    def test_validate_date_end_date_relation(self):
        today = datetime.now(IST).date()
        start = (today + timedelta(days=5)).strftime("%Y-%m-%d")
        earlier_end = (today + timedelta(days=3)).strftime("%Y-%m-%d")
        valid_end = (today + timedelta(days=7)).strftime("%Y-%m-%d")

        draft = {"start_date": start}

        # End date earlier than start date must fail
        is_valid, err, _ = validate_date("end_date", earlier_end, "To Date", draft)
        self.assertFalse(is_valid)
        self.assertIn("cannot be earlier", err)

        # End date after start date must pass
        is_valid, err, val = validate_date("end_date", valid_end, "To Date", draft)
        self.assertTrue(is_valid)
        self.assertIsNone(err)
        self.assertEqual(val, valid_end)

    def test_validate_field_options(self):
        field_def = {
            "id": "addType",
            "label": "Advertisement Type",
            "type": "dropdown",
            "options": ["Hoarding Board", "Kiosk", "Gantry"]
        }

        # Exact match
        is_valid, err, val = validate_field(field_def, "Hoarding Board")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Hoarding Board")

        # Case & space normalized match
        is_valid, err, val = validate_field(field_def, "hoarding board")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Hoarding Board")

        # Fuzzy match for spelling mistakes (e.g. 'horading' -> 'Hoarding Board')
        is_valid, err, val = validate_field(field_def, "horading")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Hoarding Board")

        # Partial word match (e.g. 'jor' for 'Jor Bagh')
        loc_def = {
            "id": "location",
            "label": "Location",
            "type": "dropdown",
            "options": ["Jor Bagh", "Green Park", "Gandhi Nagar"]
        }
        is_valid, err, val = validate_field(loc_def, "jor")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Jor Bagh")

        # Dictionary option rows (e.g. MDMS objects)
        dict_def = {
            "id": "category",
            "label": "Category",
            "type": "dropdown",
            "options": [{"name": "Street Cleaning", "code": "SC"}, {"name": "Water Line Leakage", "code": "WLL"}]
        }
        is_valid, err, val = validate_field(dict_def, "street cleaning")
        self.assertTrue(is_valid)
        self.assertEqual(val["code"], "SC")

        # Invalid option
        is_valid, err, val = validate_field(field_def, "Billboard")
        self.assertFalse(is_valid)
        self.assertIn("valid option", err)

        # Unlisted address must not falsely match a different locality
        loc_list_def = {
            "id": "locality",
            "label": "Locality",
            "type": "dropdown",
            "options": ["Hardev Nagar", "Gandhi Nagar", "Model Town"]
        }
        is_valid, err, val = validate_field(loc_list_def, "hareta marg")
        self.assertFalse(is_valid)
        self.assertIn("valid option", err)

    def test_validate_field_mobile(self):
        field_def = {
            "id": "mobileNumber",
            "label": "Mobile Number",
            "validation": {"pattern": r"^[6-9][0-9]{9}$"}
        }

        # Clean 10-digit number
        is_valid, err, val = validate_field(field_def, "9876543210")
        self.assertTrue(is_valid)
        self.assertEqual(val, "9876543210")

        # +91 prefix with spaces
        is_valid, err, val = validate_field(field_def, "+91 9876543210")
        self.assertTrue(is_valid)
        self.assertEqual(val, "9876543210")

        # 0 prefix (11 digits)
        is_valid, err, val = validate_field(field_def, "09876543210")
        self.assertTrue(is_valid)
        self.assertEqual(val, "9876543210")

        # Invalid number (starts with 3)
        is_valid, err, _ = validate_field(field_def, "3876543210")
        self.assertFalse(is_valid)
        self.assertIn("10-digit Indian mobile number", err)

        # Invalid length
        is_valid, err, _ = validate_field(field_def, "98765")
        self.assertFalse(is_valid)

    def test_validate_field_email(self):
        field_def = {
            "id": "emailId",
            "label": "Email Address",
            "validation": {"pattern": r"^[a-zA-Z0-9_.+-]+@[a-zA-Z][a-zA-Z0-9-]*(\.[a-zA-Z]{2,})+$"}
        }

        # Valid email with mixed-case domain
        is_valid, err, val = validate_field(field_def, "Citizen.Name@Example.COM")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Citizen.Name@example.com")

        # Invalid emails
        for invalid_email in ["not-an-email", "shailja@12gmail.com", "shai@gam.", "user@.com"]:
            is_valid, err, _ = validate_field(field_def, invalid_email)
            self.assertFalse(is_valid)
            self.assertIn("valid email address", err)

    def test_validate_field_pattern(self):
        field_def = {
            "id": "applicantName",
            "label": "Applicant Name",
            "validation": {"pattern": r"^[a-zA-Z\u0900-\u097F .'-]+$"}
        }

        # Names with initials / dots
        is_valid, err, val = validate_field(field_def, "R.K sharma")
        self.assertTrue(is_valid)
        self.assertEqual(val, "R.K sharma")

        # Standard full names
        is_valid, err, val = validate_field(field_def, "Rahul Sharma")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Rahul Sharma")

        # Devanagari / Hindi names
        is_valid, err, val = validate_field(field_def, "राहुल शर्मा")
        self.assertTrue(is_valid)
        self.assertEqual(val, "राहुल शर्मा")

        # Hyphen and apostrophe
        is_valid, err, val = validate_field(field_def, "Mary-Anne O'Connor")
        self.assertTrue(is_valid)

        # Numbers or symbols must fail
        is_valid, err, _ = validate_field(field_def, "Rahul123")
        self.assertFalse(is_valid)
        self.assertIn("Invalid format", err)

    def test_validate_field_length(self):
        field_def = {
            "id": "description",
            "label": "Description",
            "validation": {"min_length": 10, "max_length": 50}
        }

        # Too short (< 10)
        is_valid, err, _ = validate_field(field_def, "Pothole")
        self.assertFalse(is_valid)
        self.assertIn("at least 10 characters", err)

        # Valid length
        is_valid, err, val = validate_field(field_def, "Huge pothole near market gate")
        self.assertTrue(is_valid)
        self.assertEqual(val, "Huge pothole near market gate")

        # Too long (> 50)
        is_valid, err, _ = validate_field(field_def, "A" * 55)
        self.assertFalse(is_valid)
        self.assertIn("at most 50 characters", err)

    def test_validate_field_optional(self):
        field_def = {
            "id": "emailId",
            "label": "Email Address",
            "validation": {"required": False, "pattern": r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9.-]+$"}
        }

        # Empty optional field should pass
        is_valid, err, val = validate_field(field_def, "")
        self.assertTrue(is_valid)
        self.assertIsNone(err)
        self.assertIsNone(val)

        is_valid, err, val = validate_field(field_def, None)
        self.assertTrue(is_valid)
        self.assertIsNone(err)
        self.assertIsNone(val)


if __name__ == "__main__":
    unittest.main()


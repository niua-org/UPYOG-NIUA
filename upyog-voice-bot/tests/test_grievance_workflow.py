"""
tests/test_grievance_workflow.py
==============================================================================
Regression and unit test suite for the UPYOG Grievance Workflow.
Covers:
1. UTF-8 explicit encoding verification in configuration loading.
2. Fast-fail schema validation across malformed YAML scenarios.
3. StateGraph router decisions (ask_next, confirm, create, cancel, error).
4. Defensive runtime handling via error_node.
==============================================================================
"""

import os
import unittest
from unittest.mock import patch
import yaml

from workflow.grievance import (
    load_and_validate_grievance_config,
    router,
    error_node,
    ALL_FIELDS,
    FORM_FIELDS,
)


class TestGrievanceConfigLoader(unittest.TestCase):
    """Tests configuration loading, UTF-8 enforcement, and schema validation."""

    def test_actual_loader_loads_utf8_real_config(self):
        """Verify the loader successfully loads the real project config.yml."""
        form_fields, all_fields, extractable, hints = load_and_validate_grievance_config()
        self.assertEqual(len(all_fields), 4)
        self.assertEqual(all_fields, ["category", "sub_category", "description", "locality"])
        self.assertEqual(len(form_fields), 4)
        self.assertEqual(len(extractable), 4)
        self.assertIn("category", hints)

    def test_actual_loader_explicitly_specifies_utf8_encoding(self):
        """Verify that open() is called with encoding='utf-8' when reading config.yml."""
        with patch("builtins.open", wraps=open) as mock_open:
            load_and_validate_grievance_config()
            found_utf8 = any(
                call.kwargs.get("encoding") == "utf-8"
                for call in mock_open.call_args_list
                if "config.yml" in str(call.args[0])
            )
            self.assertTrue(
                found_utf8,
                "The configuration loader must explicitly specify encoding='utf-8' for cross-platform compatibility."
            )

    def test_missing_grievance_section(self):
        """Loader must reject configuration missing 'forms.grievance'."""
        bad_yaml = "forms:\n  other_form:\n    fields: []"
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("Missing 'forms.grievance' section", str(ctx.exception))

    def test_fields_is_not_a_list(self):
        """Loader must reject configuration where 'fields' is a string or invalid type."""
        bad_yaml = "forms:\n  grievance:\n    fields: 'not-a-list'"
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("'forms.grievance.fields' must be a non-empty list", str(ctx.exception))

    def test_empty_fields_list(self):
        """Loader must reject configuration where 'fields' is empty []."""
        bad_yaml = "forms:\n  grievance:\n    fields: []"
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("'forms.grievance.fields' must be a non-empty list", str(ctx.exception))

    def test_field_entry_is_none(self):
        """Loader must reject configuration where a field entry is None / null."""
        bad_yaml = """
        forms:
          grievance:
            fields:
              - id: category
                type: dropdown
                label: Category
              - null
        """
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("must be a dictionary", str(ctx.exception))

    def test_duplicate_field_ids(self):
        """Loader must reject configuration containing duplicate field IDs."""
        bad_yaml = """
        forms:
          grievance:
            fields:
              - id: category
                type: dropdown
                label: Category 1
              - id: category
                type: dropdown
                label: Category 2
        """
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("Duplicate field ID 'category'", str(ctx.exception))

    def test_missing_field_id(self):
        """Loader must reject a field entry that is missing an 'id'."""
        bad_yaml = """
        forms:
          grievance:
            fields:
              - label: Category Without ID
                type: dropdown
        """
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("missing a valid 'id'", str(ctx.exception))

    def test_missing_field_label(self):
        """Loader must reject a field entry that is missing a 'label'."""
        bad_yaml = """
        forms:
          grievance:
            fields:
              - id: category
                type: dropdown
        """
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("missing a valid 'label'", str(ctx.exception))

    def test_invalid_field_type(self):
        """Loader must reject a field entry with an unsupported widget type."""
        bad_yaml = """
        forms:
          grievance:
            fields:
              - id: category
                label: Category
                type: unsupported_widget
        """
        with self.assertRaises(ValueError) as ctx:
            load_and_validate_grievance_config(yaml_content=bad_yaml)
        self.assertIn("invalid widget type 'unsupported_widget'", str(ctx.exception))


class TestGrievanceRouter(unittest.TestCase):
    """Tests routing decisions inside the grievance workflow graph."""

    def test_empty_draft_routes_to_ask_next(self):
        """Empty draft must route to ask_next, NEVER directly to confirm."""
        state = {"draft_grievance": {}}
        self.assertEqual(router(state), "ask_next")

    def test_partial_draft_routes_to_ask_next(self):
        """Partially filled draft must route to ask_next."""
        state = {
            "draft_grievance": {
                "category": "StreetLights",
                "sub_category": None,
                "description": None,
                "locality": None,
            }
        }
        self.assertEqual(router(state), "ask_next")

    def test_fully_filled_draft_routes_to_confirm(self):
        """Complete draft with all fields filled must route to confirm."""
        state = {
            "draft_grievance": {
                "category": "StreetLights",
                "sub_category": "Street Light Not Working",
                "description": "Light on main pillar is broken",
                "locality": "Preet Nagar",
            }
        }
        self.assertEqual(router(state), "confirm")

    def test_confirmed_draft_routes_to_create_complaint(self):
        """Draft with _confirmed=True must route to create_complaint."""
        state = {
            "draft_grievance": {
                "category": "StreetLights",
                "sub_category": "Street Light Not Working",
                "description": "Light on main pillar is broken",
                "locality": "Preet Nagar",
                "_confirmed": True,
            }
        }
        self.assertEqual(router(state), "create_complaint")

    def test_cancelled_draft_routes_to_cancel(self):
        """Draft with _cancelled=True must route to cancel."""
        state = {
            "draft_grievance": {
                "_cancelled": True,
            }
        }
        self.assertEqual(router(state), "cancel")

    def test_empty_all_fields_routes_to_error_node(self):
        """If ALL_FIELDS is empty (vacuous truth condition), router must route to 'error'."""
        with patch("workflow.grievance.ALL_FIELDS", []):
            state = {"draft_grievance": {}}
            self.assertEqual(
                router(state),
                "error",
                "Empty ALL_FIELDS must route to 'error' to prevent false confirmation."
            )


class TestGrievanceErrorNode(unittest.TestCase):
    """Tests defensive error handling when a misconfigured state reaches runtime."""

    def test_error_node_returns_service_unavailable(self):
        """Error node must return a polite service unavailable message and clear draft."""
        state = {"phone_number": "9999999999"}
        result = error_node(state)

        self.assertIn("messages", result)
        msg_content = result["messages"][0].content
        self.assertIn("Service Temporarily Unavailable", msg_content)
        self.assertNotIn("Complaint Withdrawn", msg_content)
        self.assertEqual(result["missing_fields"], [])


if __name__ == "__main__":
    unittest.main()

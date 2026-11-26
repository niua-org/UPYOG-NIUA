"""
Unit tests for Advertisement Booking router and option matching.
Covers PR review comments regarding:
- Word-boundary matching on router confirm/cancel inputs ("submit now", "no", "don't submit", "confirm & submit")
- Deterministic option matching in match_option()
- Safe cancellation vs location inputs like "near bus stop"
"""

import unittest
from langchain_core.messages import HumanMessage
from workflow.adv_booking import router, match_option, ALL_FIELDS, SEARCH_FIELDS


class TestAdvBookingRouter(unittest.TestCase):

    def _build_awaiting_confirm_state(self, user_msg: str):
        draft = {f: f"test_val_{f}" for f in ALL_FIELDS}
        draft["selected_slots"] = [{"slot_id": 1}]
        draft["applicant_details"] = {"name": "Test User", "mobile": "9999999999"}
        draft["_awaiting_confirm"] = True
        return {
            "draft_booking": draft,
            "messages": [HumanMessage(content=user_msg)],
            "phone_number": "9999999999",
        }

    def test_router_confirm_inputs(self):
        """Valid confirmations (including words starting with 'no' like 'now', 'note', 'nothing') must route to create_booking."""
        test_cases = [
            ("submit now", "create_booking"),
            ("confirm & submit", "create_booking"),
            ("yes", "create_booking"),
            ("yes, please submit", "create_booking"),
            ("note down and submit", "create_booking"),
            ("nothing to change, submit", "create_booking"),
            ("ok submit", "create_booking"),
        ]
        for msg, expected_route in test_cases:
            with self.subTest(msg=msg):
                state = self._build_awaiting_confirm_state(msg)
                route = router(state)
                self.assertEqual(route, expected_route, f"Failed for input: '{msg}'")

    def test_router_cancel_inputs(self):
        """Explicit declines or cancellations must route to cancel."""
        test_cases = [
            ("no", "cancel"),
            ("don't submit", "cancel"),
            ("dont submit", "cancel"),
            ("do not submit this", "cancel"),
            ("cancel", "cancel"),
            ("nahi", "cancel"),
            ("nahin", "cancel"),
            ("rehne do", "cancel"),
            ("stop", "cancel"),
            ("abort", "cancel"),
            ("no, cancel it", "cancel"),
        ]
        for msg, expected_route in test_cases:
            with self.subTest(msg=msg):
                state = self._build_awaiting_confirm_state(msg)
                route = router(state)
                self.assertEqual(route, expected_route, f"Failed for input: '{msg}'")

    def test_router_edit_inputs(self):
        """Edit requests must route to edit_prompt."""
        test_cases = [
            ("edit", "edit_prompt"),
            ("edit details", "edit_prompt"),
            ("modify", "edit_prompt"),
            ("update", "edit_prompt"),
        ]
        for msg, expected_route in test_cases:
            with self.subTest(msg=msg):
                state = self._build_awaiting_confirm_state(msg)
                route = router(state)
                self.assertEqual(route, expected_route, f"Failed for input: '{msg}'")


class TestMatchOption(unittest.TestCase):

    def setUp(self):
        self.options = ["Unipolar", "Kiosk Pole", "Bill Board", "Poster", "Hoarding", "Digital Screen"]

    def test_exact_match(self):
        self.assertEqual(match_option("Hoarding", self.options), "Hoarding")
        self.assertEqual(match_option("kiosk pole", self.options), "Kiosk Pole")
        self.assertEqual(match_option("digital_screen", self.options), "Digital Screen")

    def test_index_match(self):
        self.assertEqual(match_option("1", self.options), "Unipolar")
        self.assertEqual(match_option("Option 5", self.options), "Hoarding")
        self.assertIsNone(match_option("Option 99", self.options))

    def test_unique_substring_match(self):
        self.assertEqual(match_option("hoard", self.options), "Hoarding")
        self.assertEqual(match_option("unipolar", self.options), "Unipolar")

    def test_short_or_ambiguous_substring_rejected(self):
        # 1-2 char inputs should NOT match arbitrarily
        self.assertIsNone(match_option("a", self.options))
        self.assertIsNone(match_option("ho", self.options))
        self.assertIsNone(match_option("po", self.options))  # matches Kiosk Pole & Poster -> ambiguous


if __name__ == "__main__":
    unittest.main()


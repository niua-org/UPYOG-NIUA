"""
Unit tests for services/normalizer.py.
Validates:
1. Entity protection & masking (tracking IDs, phone numbers, dates)
2. Voice ASR acoustic & phonetic correction
3. Domain-constrained fuzzy spell correction
4. Damerau-Levenshtein distance calculation and thresholding
5. Service synonym resolution
"""

import unittest
from services.normalizer import (
    damerau_levenshtein_distance,
    find_closest_domain_token,
    normalize_voice_and_text,
    resolve_service_by_synonyms,
    get_domain_vocabulary,
    load_synonyms_registry
)


class TestNormalizer(unittest.TestCase):
    """Test suite for voice ASR normalization, fuzzy spell checking, and synonyms."""

    def test_damerau_levenshtein_distance(self):
        # Exact match
        self.assertEqual(damerau_levenshtein_distance("complaint", "complaint"), 0)
        # 1 insertion
        self.assertEqual(damerau_levenshtein_distance("complain", "complaint"), 1)
        # 1 deletion
        self.assertEqual(damerau_levenshtein_distance("complaint", "complain"), 1)
        # 1 substitution
        self.assertEqual(damerau_levenshtein_distance("helo", "hello"), 1)
        # 1 transposition
        self.assertEqual(damerau_levenshtein_distance("greivance", "grievance"), 1)
        self.assertEqual(damerau_levenshtein_distance("complnt", "complaint"), 2)

    def test_entity_protection(self):
        # Tracking IDs, phone numbers, and dates must NEVER be altered
        query = "Track my status for PGR-2024-0012 with mobile 9876543210 on 2026-11-23"
        normalized, has_changes, audit = normalize_voice_and_text(query)
        self.assertIn("PGR-2024-0012", normalized)
        self.assertIn("9876543210", normalized)
        self.assertIn("2026-11-23", normalized)

    def test_bypass_json_payloads(self):
        # Structured document upload payload must NEVER be mutated (e.g. 'document' -> 'documents')
        doc_payload = '{"document": "252d4fe7-4dbd-46d8-8381-617f98c70ff5"}'
        normalized, has_changes, audit = normalize_voice_and_text(doc_payload)
        self.assertFalse(has_changes)
        self.assertEqual(normalized, doc_payload)
        self.assertIn('"document"', normalized)

        # Array JSON payload
        arr_payload = '[{"slot_id": 1, "status": "selected"}]'
        norm_arr, changed_arr, _ = normalize_voice_and_text(arr_payload)
        self.assertFalse(changed_arr)
        self.assertEqual(norm_arr, arr_payload)

    def test_asr_acoustic_replacements(self):
        # Spaced compound words from ASR
        q1 = "I want to book a uni pole in Indiranagar"
        norm1, changed1, _ = normalize_voice_and_text(q1)
        self.assertTrue(changed1)
        self.assertIn("unipole", norm1)
        self.assertNotIn("uni pole", norm1)

        # ASR homophone 'add booking'
        q2 = "I want an add booking"
        norm2, changed2, _ = normalize_voice_and_text(q2)
        self.assertTrue(changed2)
        self.assertIn("ad booking", norm2)

        # Transliteration variations
        q3 = "meri gali me shikayet hai aur kachada pada hai"
        norm3, changed3, _ = normalize_voice_and_text(q3)
        self.assertTrue(changed3)
        self.assertIn("shikayat", norm3)
        self.assertIn("kachra", norm3)

    def test_fuzzy_spell_correction(self):
        # Keyboard fat-finger typo
        q1 = "I want to file a complnt regarding water supply"
        norm1, changed1, audit1 = normalize_voice_and_text(q1)
        self.assertTrue(changed1)
        self.assertIn("complaint", norm1)

        # Transposition typo
        q2 = "I want to file a greivance"
        norm2, changed2, audit2 = normalize_voice_and_text(q2)
        self.assertTrue(changed2)
        self.assertIn("grievance", norm2)

        # Greeting typo
        q3 = "helo how are you"
        norm3, changed3, audit3 = normalize_voice_and_text(q3)
        self.assertTrue(changed3)
        self.assertIn("hello", norm3)

        # Advertisement typo
        q4 = "book a horading board"
        norm4, changed4, audit4 = normalize_voice_and_text(q4)
        self.assertTrue(changed4)
        self.assertIn("hoarding", norm4)

    def test_preserve_unrelated_and_short_words(self):
        # Short words (<= 3 chars) and arbitrary words outside domain should NOT be corrupted
        q = "go to the new park in the city"
        norm, changed, _ = normalize_voice_and_text(q)
        # Should remain practically unchanged
        self.assertEqual(norm, q)

    def test_service_synonym_resolution(self):
        # English synonym
        res1 = resolve_service_by_synonyms("I want to rent a billboard for marketing")
        self.assertIsNotNone(res1)
        self.assertEqual(res1["service_key"], "adv_booking")
        self.assertEqual(res1["target_workflow"], "adv_booking")

        # Hinglish advertisement expression
        res2 = resolve_service_by_synonyms("mujhe banner lagwana hai market me")
        self.assertIsNotNone(res2)
        self.assertEqual(res2["service_key"], "adv_booking")

        # Devanagari advertisement expression
        res3 = resolve_service_by_synonyms("शहर में होर्डिंग की बुकिंग करनी है")
        self.assertIsNotNone(res3)
        self.assertEqual(res3["service_key"], "adv_booking")

        # Civic subcategory: garbage
        res4 = resolve_service_by_synonyms("gali me kachra pada hai safai karwao")
        self.assertIsNotNone(res4)
        self.assertEqual(res4["service_key"], "grievance")
        self.assertEqual(res4["target_workflow"], "grievance")

        # Civic subcategory: street light
        res5 = resolve_service_by_synonyms("street light band hai raat ko")
        self.assertIsNotNone(res5)
        self.assertEqual(res5["service_key"], "grievance")

        # Property tax colloquial expression
        res6 = resolve_service_by_synonyms("ghar patti kaise jama kare")
        self.assertIsNotNone(res6)
        self.assertEqual(res6["service_key"], "property_tax")
        self.assertEqual(res6["target_workflow"], "faq")

        # Trade license colloquial expression
        res7 = resolve_service_by_synonyms("dukan license renew karwana hai")
        self.assertIsNotNone(res7)
        self.assertEqual(res7["service_key"], "trade_license")
        self.assertEqual(res7["target_workflow"], "faq")

    def test_municipal_abbreviation_expansion(self):
        # PT abbreviation expansion
        q1 = "PT ka bill check karna hai"
        norm1, changed1, audit1 = normalize_voice_and_text(q1)
        self.assertTrue(changed1)
        self.assertIn("Property Tax (PT)", norm1)

        # TL abbreviation expansion
        q2 = "TL renew karwane ki fees kitni hai"
        norm2, changed2, audit2 = normalize_voice_and_text(q2)
        self.assertTrue(changed2)
        self.assertIn("Trade License (TL)", norm2)

        # WS abbreviation expansion
        q3 = "WS new connection apply karna hai"
        norm3, changed3, audit3 = normalize_voice_and_text(q3)
        self.assertTrue(changed3)
        self.assertIn("Water & Sewerage (WS)", norm3)

        # PGR abbreviation expansion
        q4 = "track my PGR status"
        norm4, changed4, audit4 = normalize_voice_and_text(q4)
        self.assertTrue(changed4)
        self.assertIn("Public Grievance Redressal (PGR)", norm4)

        # Property ID (PID) expansion
        q5 = "check my PID details"
        norm5, changed5, audit5 = normalize_voice_and_text(q5)
        self.assertTrue(changed5)
        self.assertIn("Property ID (PID)", norm5)

        # Critical Guard: Tracking IDs with hyphens (e.g. PT-2024-0012) must remain intact
        q6 = "my application number is PT-2024-0012"
        norm6, _, _ = normalize_voice_and_text(q6)
        self.assertIn("PT-2024-0012", norm6)
        self.assertNotIn("Property Tax (PT)-2024-0012", norm6)

    def test_colloquial_slang_resolution(self):
        # Sanitation slang: kachrewala nahi aaya
        res1 = resolve_service_by_synonyms("kachrewala subah se nahi aaya kooda pada hai")
        self.assertIsNotNone(res1)
        self.assertEqual(res1["service_key"], "grievance")

        # Sanitation slang: safai wale nahi aaye
        res2 = resolve_service_by_synonyms("safai wale nahi aaye gali me kachra hai")
        self.assertIsNotNone(res2)
        self.assertEqual(res2["service_key"], "grievance")

        # Lighting slang: andhera pada hai
        res3 = resolve_service_by_synonyms("gali me andhera pada hai light band hai")
        self.assertIsNotNone(res3)
        self.assertEqual(res3["service_key"], "grievance")

        # Animal nuisance: awarah kutte
        res4 = resolve_service_by_synonyms("colony me awarah kutte ghoom rahe hain report karo")
        self.assertIsNotNone(res4)
        self.assertEqual(res4["service_key"], "grievance")

        # Trade License slang: gumasta license
        res5 = resolve_service_by_synonyms("dukan ka gumasta license banwana hai")
        self.assertIsNotNone(res5)
        self.assertEqual(res5["service_key"], "trade_license")

        # Property Tax slang: naam chadwana (mutation)
        res6 = resolve_service_by_synonyms("property me naam chadwana hai process batao")
        self.assertIsNotNone(res6)
        self.assertEqual(res6["service_key"], "property_tax")

        # Property Tax slang: ghar ki pauti
        res7 = resolve_service_by_synonyms("ghar ki pauti download karni hai")
        self.assertIsNotNone(res7)
        self.assertEqual(res7["service_key"], "property_tax")

        # Advertisement slang: chauraha par board
        res8 = resolve_service_by_synonyms("chauraha par board lagana hai")
        self.assertIsNotNone(res8)
        self.assertEqual(res8["service_key"], "adv_booking")


if __name__ == "__main__":
    unittest.main()

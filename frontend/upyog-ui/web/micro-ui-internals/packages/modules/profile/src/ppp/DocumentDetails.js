import React, { useRef, useState } from "react";
import { FormStep, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import EditDocuments from "../components/EditDocuments";
import FamilyCreateTabs from "../config/FamilyCreateTabs";
import { useProfileUser } from "../utils";

/**
 * DocumentDetails — last step ("Documents") of the Create Family flow.
 *
 * Reuses EditDocuments (Identity / DOB proof upload) with its own Save hidden; "Save & Next"
 * (FormStep submit bar) saves the documents and hands over to FamilyCreate, whose `nextStep: null`
 * redirects to the Family Members page.
 */
const DocumentDetails = ({ t, config, onSelect }) => {
  const { user, loading } = useProfileUser();
  const documentsRef = useRef(null);
  const [saving, setSaving] = useState(false);

  const handleNext = async () => {
    if (saving) return;
    setSaving(true);
    const saved = await documentsRef.current?.save({ proceedIfUnchanged: true });
    setSaving(false);
    if (saved) onSelect(config.key, { completed: true }, false);
  };

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />
      <FormStep config={config} onSelect={handleNext} isDisabled={saving} t={t}>
        <EditDocuments ref={documentsRef} hideSave userDetails={user || {}} />
      </FormStep>
    </React.Fragment>
  );
};

export default DocumentDetails;

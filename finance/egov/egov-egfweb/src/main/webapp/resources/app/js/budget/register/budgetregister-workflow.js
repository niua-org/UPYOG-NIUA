


$('.btn-wf-primary').click(function(){
	var button = $(this).attr('id');
	if (button != null && (button == 'Forward')) {
		if(!validateWorkFlowApprover(button))
			return false;
		if(!$("form").valid())
			return false;
		if(validate()){
			deleteHiddenSubledgerRow();
			return true;
		}else
			return false;

	}else if (button != null && (button == 'Create And Approve')) {
		$('#approvalDepartment').removeAttr('required');
		$('#approvalDesignation').removeAttr('required');
		$('#approvalPosition').removeAttr('required');
		$('#approvalComent').removeAttr('required');
		if(!validateWorkFlowApprover(button))
			return false;
		if(!$("form").valid())
			return false;
		if(validate()){
			deleteHiddenSubledgerRow();
			return true;
		}else
			return false;
	} else{
		if(!validateWorkFlowApprover(button))
			return false;
		if($("form").valid()){
			deleteHiddenSubledgerRow();
			return true;
		}else
			return false;
	}
	return false;
});


function validateWorkFlowApprover(name) {
	document.getElementById("workFlowAction").value = name;
	var button = document.getElementById("workFlowAction").value;
	if (button != null && button == 'Submit') {
		$('#approvalDepartment').attr('required', 'required');
		$('#approvalDesignation').attr('required', 'required');
		$('#approvalPosition').attr('required', 'required');
		$('#approvalComent').removeAttr('required');
	}
	if (button != null && button == 'Reject') {
		$('#approvalDepartment').removeAttr('required');
		$('#approvalDesignation').removeAttr('required');
		$('#approvalPosition').removeAttr('required');
		$('#approvalComent').attr('required', 'required');
	}
	if (button != null && button == 'Cancel') {
		$('#approvalDepartment').removeAttr('required');
		$('#approvalDesignation').removeAttr('required');
		$('#approvalPosition').removeAttr('required');
		$('#approvalComent').attr('required', 'required');
	}
	if (button != null && button == 'Forward') {
		$('#approvalDepartment').attr('required', 'required');
		$('#approvalDesignation').attr('required', 'required');
		$('#approvalPosition').attr('required', 'required');
		$('#approvalComent').removeAttr('required');
	}
	if (button != null && button == 'Approve') {
		$('#approvalComent').removeAttr('required');
	}
	if (button != null && button == 'Revert') {
        $('#approvalDepartment').removeAttr('required');
        $('#approvalDesignation').removeAttr('required');
        $('#approvalPosition').removeAttr('required');
	    $('#approvalComent').attr('required', 'required');
	}
	if (button != null && button == 'Create And Approve') {
		return validateCutOff();
	}else
		return true;

	return true;
}
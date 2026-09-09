        function switchTab(tab, e) {

    document.querySelectorAll('.tab')
        .forEach(t => t.classList.remove('active'));

    document.querySelectorAll('.tab-content')
        .forEach(c => c.classList.remove('active'));

    e.target.classList.add('active');

    document.getElementById(tab)
        .classList.add('active');
}

 // =========================
    // EXECUTION PROGRESS
    // =========================

    let progressInterval = null;

    function startProgressPolling() {

        if (progressInterval) {
            clearInterval(progressInterval);
        }

        progressInterval = setInterval(async function () {

            try {

                const response =
                    await fetch("/api/module/progress");

                if (!response.ok) {
                    return;
                }

                const progress =
                    await response.json();

                const total =
                    progress.totalTestCases || 0;

                const completed =
                    progress.completedTestCases || 0;

                const currentTest =
                    progress.currentTestCase || "";

                if (total > 0) {

                    const currentNumber =
                        Math.min(completed + 1, total);

                    const percentage =
                        Math.round(
                            (completed / total) * 100
                        );

                    document.getElementById(
                        "executionProgressText"
                    ).innerText =
                        "Running Test Case "
                        + currentNumber
                        + " of "
                        + total;

                    document.getElementById(
                        "executionCurrentTest"
                    ).innerText =
                        currentTest
                            ? progress.currentModule
                              + " — "
                              + currentTest
                            : "Executing test case...";

                    document.getElementById(
                        "executionProgressBar"
                    ).style.width =
                        percentage + "%";

                    document.getElementById(
                        "executionProgressPercentage"
                    ).innerText =
                        percentage + "%";
                }

                if (!progress.executionRunning) {

                    clearInterval(progressInterval);
                    progressInterval = null;

                    if (total > 0) {

                        document.getElementById(
                            "executionProgressText"
                        ).innerText =
                            "Execution Completed — "
                            + total
                            + " of "
                            + total;

                        document.getElementById(
                            "executionCurrentTest"
                        ).innerText =
                            "All test cases completed.";

                        document.getElementById(
                            "executionProgressBar"
                        ).style.width = "100%";

                        document.getElementById(
                            "executionProgressPercentage"
                        ).innerText = "100%";
                    }
                }

            } catch (error) {

                console.error(
                    "Progress polling failed:",
                    error
                );

            }

        }, 500);
    }

         // =========================
         // MODULE FORM SUBMIT
         // =========================

        document.getElementById('moduleForm').addEventListener('submit', async (e) => {

    e.preventDefault();

    const runBtn = document.getElementById("runBtn");
    const statusDiv = document.getElementById("testStatus");
    const statusSpinner = document.getElementById("statusSpinner");
    const statusText = document.getElementById("statusText");
    const statusSubText = document.getElementById("statusSubText");

    // Start Status
    runBtn.disabled = true;
    runBtn.innerHTML = "⏳ Running...";

    statusDiv.style.display = "block";
    statusSpinner.style.display = "block";

    statusText.innerHTML = "⏳ Test Started...";
    statusSubText.innerHTML =
        "Running automation. Please don't refresh this page.";

    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);

    const selectedModules =
        Array.from(
            document.querySelectorAll(
                '#moduleOptions1 input[name="moduleTestingModules"]:checked'
            )
        ).map(cb => cb.value);

    data.moduleName = selectedModules.join(",");

    try {

    // Show execution progress

const executionProgress =
    document.getElementById("executionProgress");

executionProgress.style.display = "block";

document.getElementById("executionProgressText").innerText =
    "Preparing test execution...";

document.getElementById("executionCurrentTest").innerText =
    "Reading test cases...";

document.getElementById("executionProgressBar").style.width =
    "0%";

document.getElementById("executionProgressPercentage").innerText =
    "0%";

    startProgressPolling();

        const response = await fetch('/api/module/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

       const result =
    await response.json();

       let html = "<h3>Execution Summary</h3>";

let pass = 0;
let fail = 0;

result.forEach(r => {

    if (r.status === "PASS") {
        pass++;
    } else {
        fail++;
    }

    const moduleName = r.module
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, c => c.toUpperCase());

  html += `
<div style="
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin:10px 0;
    padding:10px 15px;
    border:1px solid #ddd;
    border-radius:8px;
    background:#fafafa;
">

    <strong>${moduleName}</strong>

    <span style="
        padding:5px 12px;
        border-radius:20px;
        font-weight:bold;
        color:white;
        background:${r.status === "PASS" ? "#28a745" : "#dc3545"};
    ">
        ${r.status}
    </span>

</div>
    `;
});

html += `
<hr>

<div style="display:flex; gap:15px; margin-top:15px;">

    <div style="padding:10px 18px; background:#e9ecef; border-radius:8px;">
        <strong>Total:</strong> ${result.length}
    </div>

    <div style="padding:10px 18px; background:#d4edda; color:#155724; border-radius:8px;">
        <strong>Passed:</strong> ${pass}
    </div>

    <div style="padding:10px 18px; background:#f8d7da; color:#721c24; border-radius:8px;">
        <strong>Failed:</strong> ${fail}
    </div>

</div>
`;

document.getElementById("moduleResult").innerHTML = html;
document.getElementById("moduleReportButtons").style.display = "block";
if (fail > 0) {

    statusText.innerHTML =
        "❌ Test Failed";

    statusSubText.innerHTML =
        `${fail} module(s) failed. Please check the report for details.`;

} else {

    statusText.innerHTML =
        "✅ Test Completed Successfully";

    statusSubText.innerHTML =
        "Automation completed successfully. Report is ready to view or download.";

}

    } catch (error) {

        // Failure

        statusText.innerHTML =
            "❌ Test Failed";

        statusSubText.innerHTML =
            error.message;

    } finally {

        // Mark execution progress as completed
        const progressBar =
            document.getElementById("executionProgressBar");

        const progressPercentage =
            document.getElementById("executionProgressPercentage");

        const progressText =
            document.getElementById("executionProgressText");

        const currentTest =
            document.getElementById("executionCurrentTest");

        if (progressBar) {
            progressBar.style.width = "100%";
        }

        if (progressPercentage) {
            progressPercentage.innerText = "100%";
        }

        if (progressText) {
            progressText.innerText = "Execution Completed";
        }

        if (currentTest) {
            currentTest.innerText = "All test cases completed.";
        }

        statusSpinner.style.display = "none";

        runBtn.disabled = false;
        runBtn.innerHTML = "Run Test";
    }

});

         // =========================
         // CITIZEN FORM SUBMIT
         // =========================

        document.getElementById('citizenForm').addEventListener('submit', async (e) => {
            e.preventDefault();
           const formData = new FormData(e.target);

const selectedModules =
    Array.from(
        document.querySelectorAll(
            '#moduleOptions input[name="modules"]:checked'
        )
    ).map(cb => cb.value);

const data = Object.fromEntries(formData);

// Send comma-separated values to backend
data.moduleName = selectedModules.join(",");

            const resultDiv = document.getElementById('citizenResult');
            resultDiv.style.display = 'block';
            resultDiv.className = 'result';
            resultDiv.textContent = 'Running test...';
            document.getElementById(
    "citizenReportButtons"
).style.display = "block";

            try {
                window.open('http://65.0.8.57:8000', '_blank');

                const response = await fetch('/api/test/citizen', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                const result = await response.text();
                resultDiv.className = 'result success';
                resultDiv.textContent = result;
            } catch (error) {
                resultDiv.className = 'result error';
                resultDiv.textContent = 'Error: ' + error.message;
            }
        });

       // =========================
       // EMPLOYEE FORM SUBMIT
       // =========================

        document.getElementById('employeeForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData);

            const resultDiv = document.getElementById('employeeResult');
            resultDiv.style.display = 'block';
            resultDiv.className = 'result';
            resultDiv.textContent = 'Running test...';
            document.getElementById(
    "employeeReportButtons"
).style.display = "block";

            try {
                window.open('http://65.0.8.57:8080', '_blank');

                const response = await fetch('/api/test/employee', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                const result = await response.text();
                resultDiv.className = 'result success';
                resultDiv.textContent = result;
            } catch (error) {
                resultDiv.className = 'result error';
                resultDiv.textContent = 'Error: ' + error.message;
            }
        });

        // =========================
       // VENDOR FORM SUBMIT
       // =========================
       document.getElementById('vendorForm').addEventListener('submit', async (e) => {
           e.preventDefault();
           const formData = new FormData(e.target);
           const data = Object.fromEntries(formData);


           const resultDiv = document.getElementById('vendorResult');
           resultDiv.style.display = 'block';
           resultDiv.className = 'result';
           resultDiv.textContent = 'Running test...';
           document.getElementById(
    "vendorReportButtons"
).style.display = "block";


           try {
           window.open('http://65.0.8.57:8000', '_blank');
               const response = await fetch('/api/test/vendor', {
                   method: 'POST',
                   headers: { 'Content-Type': 'application/json' },
                   body: JSON.stringify(data)
               });
               const result = await response.text();
               resultDiv.className = 'result success';
               resultDiv.textContent = result;
           } catch (error) {
               resultDiv.className = 'result error';
               resultDiv.textContent = 'Error: ' + error.message;
           }
       });
       // =========================
       // AUTO-FILL LOGIC + MULTI-SELECT DROPDOWN
       // =========================

// Employee/Vendor dropdown autofill

document.querySelectorAll("select[name='moduleName']").forEach(select => {
    select.addEventListener("change", function () {

        const form = this.closest("form");

        const mobileField = form.querySelector("input[name='mobileNumber']");
        const otpField = form.querySelector("input[name='otp']");
        const baseUrlField = form.querySelector("[name='baseUrl']");

        if (!mobileField || !otpField) return;

        let mobile = "7906413996";
        let otp = "123456";

        const baseUrl = baseUrlField ? baseUrlField.value : "";

        if (
            this.value === "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM" ||
            this.value === "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC"
        ) {
            if (baseUrl.includes("niuatt.niua")) {
                mobile = "7272727216";
            } else if (baseUrl.includes("upyog.niua")) {
                mobile = "8888888881";
            }
        }

        mobileField.value = mobile;
        otpField.value = otp;
    });
});

// Citizen multi-select dropdown

function toggleDropdownById(dropdownId, event) {
    event.stopPropagation();
    document.getElementById(dropdownId).classList.toggle("show");
}
function toggleDropdown(event) {
    event.stopPropagation();
    document.getElementById("moduleDropdown").classList.toggle("show");
}

function filterModules() {
    const search = document.getElementById("moduleSearch").value.toLowerCase();
    const labels = document.querySelectorAll("#moduleOptions label");

    labels.forEach(label => {
        const text = label.textContent.toLowerCase();
        label.style.display = text.includes(search)
            ? "flex"
            : "none";
    });
}
function filterReportModules() {

    const search = document
        .getElementById("moduleSearch2")
        .value
        .toLowerCase();

    const labels = document.querySelectorAll(
        "#moduleOptions2 label"
    );

    labels.forEach(label => {

        const text = label.textContent.toLowerCase();

        label.style.display =
            text.includes(search)
                ? "flex"
                : "none";
    });
}

document.querySelectorAll("input[name='modules']").forEach(cb => {

    cb.addEventListener("click", function(e) {
        e.stopPropagation();
    });

    cb.addEventListener("change", function () {
        updateSelectedModules();
        autoFillCitizenFields(this);
    });

});

function updateSelectedModules() {
    const selected = Array.from(
        document.querySelectorAll("input[name='modules']:checked")
    ).map(cb => cb.parentElement.textContent.trim());

    document.getElementById("selectedModulesText").textContent =
        selected.length > 0
            ? selected.join(", ")
            : "Select Modules";
}
document.querySelectorAll(
    "input[name='moduleTestingModules']"
).forEach(cb => {

    cb.addEventListener("change", function () {
        updateSelectedModulesModule();
    });

});

document.querySelectorAll(
    '#moduleOptions2 input[name="reportModules"]'
).forEach(cb => {
    cb.addEventListener("change", updateSelectedReportModules);
});

function updateSelectedModulesModule() {

    const selected = Array.from(
        document.querySelectorAll(
            "input[name='moduleTestingModules']:checked"
        )
    ).map(cb => cb.parentElement.textContent.trim());

    document.getElementById("selectedModulesText1").textContent =
        selected.length > 0
            ? selected.join(", ")
            : "Select Modules";
}

function updateSelectedReportModules() {

   const checked = document.querySelectorAll(
    '#moduleOptions2 input[name="reportModules"]:checked'
);

    const text = document.getElementById("selectedModulesReport");

    if (checked.length === 0) {
        text.innerHTML = "Select Modules";
        return;
    }

    text.innerHTML = Array.from(checked)
        .map(c => c.parentElement.textContent.trim())
        .join(", ");
}

async function loadReports() {

    const selectedModules = Array.from(
        document.querySelectorAll(
            '#moduleOptions2 input[name="reportModules"]:checked'
        )
    ).map(cb => cb.value);

    if (selectedModules.length === 0) {
        alert("Please select at least one module.");
        return;
    }

    const tbody = document.getElementById("reportTableBody");
    tbody.innerHTML = "";

   const responses = await Promise.all(

    selectedModules.map(async module => {

        const response =
            await fetch(`/api/report/module/${module}`);

        const reports =
            await response.json();

        return reports.map(r => ({
            ...r,
            module
        }));

    })

);

const allReports = responses.flat();

    allReports.sort((a, b) => {

        const d1 = new Date(`${a.date} ${a.time}`);
        const d2 = new Date(`${b.date} ${b.time}`);

        return d2 - d1;

    });

    allReports.forEach(report => {

tbody.innerHTML += `
<tr style="
    background:${report.status === "PASS"
        ? "#d4edda"
        : "#f8d7da"};
">

    <td>${report.module}</td>

    <td>${report.date}</td>

    <td>${report.time}</td>

    <td>
        <b style="
            color:${report.status === "PASS"
                ? "green"
                : "red"};
        ">
            ${report.status}
        </b>
    </td>

    <td>
        <a href="/api/report/view/${report.fileName}" target="_blank">
            👁 View
        </a>
    </td>

    <td>
        <a href="/api/report/download/${report.fileName}">
            ⬇ Download
        </a>
    </td>

</tr>
`;
});
document.getElementById("reportTableContainer").style.display = "block";

}

function autoFillCitizenFields(moduleCheckbox) {
    const form = moduleCheckbox.closest("form");

    const mobileField = form.querySelector("input[name='mobileNumber']");
    const otpField = form.querySelector("input[name='otp']");
    const baseUrlField = form.querySelector("[name='baseUrl']");

    if (!mobileField || !otpField) return;

    let mobile = "7906413996";
    let otp = "123456";

    const baseUrl = baseUrlField ? baseUrlField.value : "";
    const selectedModule = moduleCheckbox.value;

    if (
        selectedModule === "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM" ||
        selectedModule === "ONLINE_BUILDING_PLAN_APPROVAL_SYSTEM_OC"
    ) {
        if (baseUrl.includes("niuatt.niua")) {
            mobile = "7272727216";
        } else if (baseUrl.includes("upyog.niua")) {
            mobile = "8888888881";
        }
    }
     if (
        selectedModule === "STREET_VENDING"
         ) {
        if (baseUrl.includes("niuatt.niua.in/sv-ui/citizen/login")) {
            mobile = "8010012414";
        }
     }

    mobileField.value = mobile;
    otpField.value = otp;

    mobileField.dispatchEvent(new Event('input', { bubbles: true }));
    mobileField.dispatchEvent(new Event('change', { bubbles: true }));
}

// Outside click close for all multi-select dropdowns
document.addEventListener("click", function (e) {

    const dropdowns = [
        "moduleDropdown",
        "moduleDropdown1",
        "moduleDropdown2"
    ];

    dropdowns.forEach(function (dropdownId) {

        const dropdown =
            document.getElementById(dropdownId);

        if (!dropdown) return;

        const container =
            dropdown.closest(".multi-select-container");

        if (
            !container ||
            !container.contains(e.target)
        ) {
            dropdown.classList.remove("show");
        }
    });
});

        // Result Output

     async function viewLatestReport(containerId) {

    const response = await fetch('/api/report/list');

    const reports = await response.json();

    let html = "";

    reports.forEach(report => {

        html += `
            <div>
                <a href="/api/report/view/${report}"
                   target="_blank">
                    ${report}
                </a>
            </div>
        `;
    });

    document.getElementById(containerId).innerHTML = html;
}

function downloadTestDataTemplate() {
    window.location.href = "/api/module/download-template";
}
async function handleExcelUpload(input) {

    const statusElement =
        document.getElementById("excelUploadStatus");

    if (!input.files || input.files.length === 0) {
        statusElement.innerText = "";
        return;
    }

    const file = input.files[0];

    if (!file.name.toLowerCase().endsWith(".xlsx")) {

        statusElement.innerText =
            "Please upload an Excel .xlsx file.";

        input.value = "";
        return;
    }

    statusElement.innerText =
        "Uploading " + file.name + "...";

    const formData = new FormData();

    formData.append("file", file);

    try {

        const response = await fetch(
            "/api/module/upload-excel",
            {
                method: "POST",
                body: formData
            }
        );

        const message =
            await response.text();

        if (!response.ok) {
            throw new Error(message);
        }

        statusElement.innerText =
            message;

    } catch (error) {

        statusElement.innerText =
            "Upload failed: " + error.message;

        input.value = "";
    }
}

function downloadLatestReport() {

    window.open(
        '/api/report/download',
        '_blank'
    );
}

        function downloadUpdatedExcel() {
    window.open(
        "/api/module/download-result",
        "_blank"
    );
}
$(document).ready(function () {
    // Initialize all select2 elements
    $('.select2').select2({
        width: 'resolve',
        theme: 'default',
    });

    // Dynamically remove line-height issue from rendered elements
    $('.select2-container--default .select2-selection--single .select2-selection__rendered').css('margin-top', '-8px');
    $('.select2-container .select2-selection--single .select2-selection__rendered').css('padding-left', '1px');

    // Handle location fetch
    $('#fetch-location').on('click', function () {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                function (position) {
                    $('#latitude').val(position.coords.latitude.toFixed(6));
                    $('#longitude').val(position.coords.longitude.toFixed(6));
                },
                function (error) {
                    alert(getLocationErrorMessage(error));
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
            );
        } else {
            alert("Geolocation is not supported by your browser.");
        }
    });

    // Handle file input change for image preview
    $('#picture').on('change', function (event) {
        const file = event.target.files[0];
        const previewImg = $('#preview-img');

        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                previewImg.attr('src', e.target.result).show();
            };
            reader.readAsDataURL(file);
        } else {
            previewImg.hide().attr('src', '/dist/img/default-50x50.gif');
        }
    });

    // Handle modal image preview
    $('#preview-img').on('click', function () {
        const modalImg = $('#modal-img');
        modalImg.attr('src', $(this).attr('src'));
    });

    // Function to handle geolocation errors
    function getLocationErrorMessage(error) {
        switch (error.code) {
            case error.PERMISSION_DENIED:
                return "Permission denied for accessing location. Please enable location services.";
            case error.POSITION_UNAVAILABLE:
                return "Unable to determine location. Please try again later.";
            case error.TIMEOUT:
                return "The location request timed out. Please retry.";
            default:
                return "An unknown error occurred while fetching location.";
        }
    }

    // Fetch property count
    async function fetchPropertyCount() {
        try {
            const response = await fetch('/property/properties/count');
            if (!response.ok) {
                throw new Error('Failed to fetch property count');
            }
            const count = await response.json();
            const countElement = document.getElementById('property-count');
            if (countElement) {
                countElement.innerText = count;
            }
        } catch (error) {
            console.error('Error fetching property count:', error);
            const countElement = document.getElementById('property-count');
            if (countElement) {
                countElement.innerText = "Error";
            }
        }
    }

    fetchPropertyCount();
});



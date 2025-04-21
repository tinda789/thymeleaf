document.addEventListener('DOMContentLoaded', function () {
    const modalElement = document.getElementById('deleteModal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        // hoặc gọi modal.show() nếu cần
    } else {
        console.warn('Modal element not found!');
    }
});

/* Chống nháy giao diện (Anti-FOUC) khi chuyển trang: Mặc định là Cream Mode ('light') + Khôi phục sớm Tab Admin */
(function () {
    try {
        var savedTheme = localStorage.getItem('pk_blindbox_theme') || 'light';
        if (savedTheme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
        } else {
            document.documentElement.removeAttribute('data-theme');
        }
        if (window.location && window.location.pathname.indexOf('/admin/dashboard') === 0) {
            var savedAdminTab = sessionStorage.getItem('pk_admin_active_tab');
            if (savedAdminTab && ['sets', 'rarities', 'cards', 'users'].indexOf(savedAdminTab) !== -1) {
                document.documentElement.setAttribute('data-admin-tab', savedAdminTab);
            }
        }
    } catch (e) {}
})();

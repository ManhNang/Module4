/* Chống nháy giao diện (Anti-FOUC) khi chuyển trang: Mặc định là Cream Mode ('light') */
(function () {
    try {
        var savedTheme = localStorage.getItem('pk_blindbox_theme') || 'light';
        if (savedTheme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
        } else {
            document.documentElement.removeAttribute('data-theme');
        }
    } catch (e) {}
})();

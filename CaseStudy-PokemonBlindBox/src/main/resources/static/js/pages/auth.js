/* ==================== AUTH PAGES (LOGIN & REGISTER) ==================== */
window.togglePasswordVisibility = function (inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    btn.querySelector('.icon-eye')?.classList.toggle('hidden', isHidden);
    btn.querySelector('.icon-eye-off')?.classList.toggle('hidden', !isHidden);
};

window.quickFillCredentials = function (user, pass, btn) {
    const usernameEl = document.getElementById('username');
    const passwordEl = document.getElementById('password');
    if (usernameEl) usernameEl.value = user;
    if (passwordEl) passwordEl.value = pass;
    document.querySelectorAll('.quickfill-btn').forEach(el => {
        el.classList.remove('border-rose-500', 'ring-2', 'ring-rose-500/20');
        el.classList.add('border-transparent');
    });
    if (btn) {
        btn.classList.remove('border-transparent');
        btn.classList.add('border-rose-500', 'ring-2', 'ring-rose-500/20');
    }
    const fb = document.getElementById('quickfill-feedback');
    if (fb) {
        fb.classList.remove('hidden');
        setTimeout(() => fb.classList.add('hidden'), 2500);
    }
};

window.syncThemeIcons = function () {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    document.getElementById('theme-icon-moon')?.classList.toggle('hidden', isDark);
    document.getElementById('theme-icon-sun')?.classList.toggle('hidden', !isDark);
};

window.toggleBlindboxTheme = function () {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) {
        document.documentElement.removeAttribute('data-theme');
        localStorage.setItem('pk_blindbox_theme', 'light');
    } else {
        document.documentElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('pk_blindbox_theme', 'dark');
    }
    window.syncThemeIcons();
};

window.showForgotPasswordHint = function () {
    document.getElementById('forgot-password-hint')?.classList.toggle('hidden');
};

document.addEventListener('DOMContentLoaded', () => {
    window.syncThemeIcons();
    const usernameEl = document.getElementById('username');
    const passwordEl = document.getElementById('password');
    if (usernameEl) {
        const isPrefilled = usernameEl.getAttribute('data-registered-prefill') === 'true' && usernameEl.value.trim().length > 0;
        if (isPrefilled && passwordEl) {
            passwordEl.focus();
        } else {
            usernameEl.focus();
        }
    }
});

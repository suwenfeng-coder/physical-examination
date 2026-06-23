document.querySelectorAll('.send-code').forEach(button => {
    button.addEventListener('click', async () => {
        const phoneInput = document.querySelector(button.dataset.phone);
        const form = button.closest('.sms-form');
        const message = form.querySelector('.sms-message');
        const phone = phoneInput.value.trim();

        if (!/^1\d{10}$/.test(phone)) {
            message.className = 'sms-message error';
            message.textContent = '请输入正确的11位手机号码';
            phoneInput.focus();
            return;
        }

        button.disabled = true;
        button.textContent = '发送中...';
        const body = new URLSearchParams({phone, purpose: button.dataset.purpose});
        try {
            const response = await fetch('/auth/sms/send', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body
            });
            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || '验证码发送失败');
            }
            message.className = 'sms-message success';
            message.textContent = result.devCode
                ? `开发环境验证码：${result.devCode}（5分钟有效）`
                : result.message;
            startCountdown(button, 60);
        } catch (error) {
            message.className = 'sms-message error';
            message.textContent = error.message;
            button.disabled = false;
            button.textContent = '获取验证码';
        }
    });
});

function startCountdown(button, seconds) {
    let remaining = seconds;
    button.textContent = `${remaining}秒后重试`;
    const timer = setInterval(() => {
        remaining -= 1;
        if (remaining <= 0) {
            clearInterval(timer);
            button.disabled = false;
            button.textContent = '重新获取';
        } else {
            button.textContent = `${remaining}秒后重试`;
        }
    }, 1000);
}

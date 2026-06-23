document.querySelectorAll('[data-department-lookup]').forEach(component => {
    const input = component.querySelector('.lookup-input');
    const hidden = component.querySelector('.lookup-id');
    const menu = component.querySelector('.lookup-menu');
    let timer;

    async function search() {
        const keyword = input.value.trim();
        const response = await fetch(`/departments/api/search?keyword=${encodeURIComponent(keyword)}`);
        const departments = await response.json();
        menu.replaceChildren();

        if (!departments.length) {
            const empty = document.createElement('span');
            empty.className = 'lookup-empty';
            empty.textContent = '未找到启用的科室，请先到科室管理中新增';
            menu.appendChild(empty);
        } else {
            departments.forEach(department => {
                const option = document.createElement('button');
                option.type = 'button';
                option.className = 'lookup-option';
                option.innerHTML = `<b>${escapeHtml(department.name)}</b><small>${escapeHtml(department.code || '')}</small>`;
                option.addEventListener('click', () => {
                    input.value = department.name;
                    hidden.value = department.id;
                    menu.classList.remove('open');
                });
                menu.appendChild(option);
            });
        }
        menu.classList.add('open');
    }

    input.addEventListener('focus', search);
    input.addEventListener('input', () => {
        hidden.value = '';
        clearTimeout(timer);
        timer = setTimeout(search, 180);
    });
    document.addEventListener('click', event => {
        if (!component.contains(event.target)) {
            menu.classList.remove('open');
        }
    });
});

function escapeHtml(value) {
    const element = document.createElement('span');
    element.textContent = value;
    return element.innerHTML;
}

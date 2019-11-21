const resetRem = () => {
    const e = document.documentElement,
        t = document.documentElement.clientWidth || document.body.clientWidth || window.innerWidth;
    e.style.fontSize = t / 10 + "px", window.rem = t / 10;
    rem
}
window.addEventListener("DOMContentLoaded", resetRem),
window.addEventListener("load", resetRem),
window.addEventListener("resize", resetRem);
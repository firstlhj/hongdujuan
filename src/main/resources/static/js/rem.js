const resetRem = () => {
    const e = document.documentElement,
        t = document.documentElement.clientWidth || document.body.clientWidth || window.innerWidth;
    e.style.fontSize = t / 10 + "px", window.rem = t / 10;
    rem
}
window.addEventListener("DOMContentLoaded", resetRem),
window.addEventListener("load", resetRem),
window.addEventListener("resize", resetRem);
let bodyEl = document.body
let tops = 0
function stopBodyScroll (isFixed) {
    if (isFixed) {
      tops = window.scrollY
  
      bodyEl.style.position = 'fixed'
      bodyEl.style.top = -top + 'px'
    } else {
      bodyEl.style.position = ''
      bodyEl.style.top = ''
      window.scrollTo(0, top) // 回到原先的top
    }
  }
  // window.addEventListener("touchmove", function(event) {
  //     if(event.scale !== 1) {
  //       event.preventDefault();
  //     }
  //     }, {
  //     passive: false
  //   }
  // );
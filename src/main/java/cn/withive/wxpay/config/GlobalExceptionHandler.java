package cn.withive.wxpay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception ex) throws Exception {
        ex.printStackTrace();
        log.error(ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("ex", ex);
        mav.addObject("url", req.getRequestURL());
        mav.setViewName("error/50x");
        return mav;
    }
}

window.wxpayUtil = (function (window, document, undefined) {

    var util = {
        isArray: function (arr) {
            return Object.prototype.toString.call(arr).toLowerCase() === "[object array]";
        },
        isDate: function (date) {
            return Object.prototype.toString.call(date).toLowerCase() === "[object date]";
        },
        isFunction: function (fun) {
            return Object.prototype.toString.call(fun).toLowerCase() === "[object function]";
        },
        isChineseChar: function (str) {
            const reg = /[\u4E00-\u9FA5\uF900-\uFA2D]/;
            return reg.test(str);
        },
        isPhone: function (phone) {
            return phone && (/^1[3456789]\d{9}$/.test(phone));
        },
        isTel: function (str) {
            const reg = /^0\d{2,3}-?\d{7,8}$/;
            return reg.test(str);
        },
        getQueryString: function (name) {
            var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
            var r = window.location.search.substr(1).match(reg);
            if (r !== null) return unescape(r[2]);
            return null;
        },
        getHomeUrl: function () {
            const area = this.getArea();
            return `${location.origin}/wxpay?area=${area}`;
        },
        getArea: function () {
            return localStorage.getItem('area');
        },
        trySetArea: function (code) {
            if (!code) {
                return;
            }

            let area = this.getArea('area');
            if (code !== area) {
                localStorage.setItem('area', code);
            }
        },
        setUserInfo: function (data) {
            data = JSON.stringify(data);
            localStorage.setItem('user', data);
        },
        getUserInfo: function () {
            let data = localStorage.getItem('user');
            if (!data) {
                return null;
            }
            return JSON.parse(data);
        },
        toDateString: function (time) {
            var d = new Date(time),
                month = '' + (d.getMonth() + 1),
                day = '' + d.getDate(),
                year = d.getFullYear();

            if (month.length < 2) month = '0' + month;
            if (day.length < 2) day = '0' + day;

            return [year, month, day].join('-');
        }
    };

    return util;
})(this, this.document);

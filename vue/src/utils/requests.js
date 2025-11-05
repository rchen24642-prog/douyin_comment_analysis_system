//引入
import axios from "axios";
import router from "@/router";

//创建一个axios对象
const request = axios.create({
    baseURL: "http://localhost:9090", //与后端端口号保持一致
    timeout: 5000
})

//request 拦截器
request.interceptors.request.use(config => {
    // 只有非 FormData 时才设置 JSON
    if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }

    const token = localStorage.getItem("token");
    const uuid = localStorage.getItem("uuid");   // ✅ 新增
    if (token){
        config.headers['token'] = token; //如果有token，把token作为请求头
    }
    if (uuid) {
        config.headers['uuid'] = uuid;           // ✅ 新增
    }

    return config
}, error => {
    return Promise.reject(error)
});

//response 拦截器
request.interceptors.response.use(
    response => {
        // response.data是后端返回的result
        let res = response.data;
        //兼容服务端返回的字符串数据，如果数据是字符串，就转成对象
        if (typeof res === 'string') {
            res =res ? JSON.parse(res) : res
        }
        //如果权限验证不通过，跳转到登录页面
        if (res.code === '401') {
            router.push("/login").catch(() => {})
        }
        return res;
        },
    error => {
        console.log('err' + error) //for debug
        return Promise.reject(error)
    }
)

export default request










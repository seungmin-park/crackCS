import "normalize.css";
import "element-plus/dist/index.css";
import "bootstrap/dist/css/bootstrap-utilities.css";
import "@/styles/main.scss";

import ElementPlus from "element-plus";
import {createApp} from "vue";

import App from "@/App.vue";
import router from "@/router";

createApp(App).use(router).use(ElementPlus).mount("#app");

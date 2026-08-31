import "normalize.css";
import "bootstrap/dist/css/bootstrap-utilities.css";
import "@/styles/main.scss";

import {createApp} from "vue";

import App from "@/App.vue";
import router from "@/router";

createApp(App).use(router).mount("#app");

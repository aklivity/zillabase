import axios from 'axios';
import { boot } from 'quasar/wrappers'
import { hideLoader, showLoader } from 'src/services/loading';

export default boot(({ app }) => {
    axios.interceptors.request.use(
        (config) => {
            showLoader();
            return config;
        },
        (error) => {
            hideLoader();
            return Promise.reject(error);
        }
    );

    axios.interceptors.response.use(
        (response) => {
            hideLoader();
            return response;
        },
        (error) => {
            hideLoader();
            return Promise.reject(error);
        }
    );
})
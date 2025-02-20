
const showLoader = (message = 'Please wait...') => {
    if (window.zillaApp) {
        window.zillaApp.$q.loading.show({
            message: message,
            html: true
        })
    }
}

const hideLoader = () => {
    if (window.zillaApp) {
        window.zillaApp.$q.loading.hide()
    }
}

export {
    showLoader,
    hideLoader
}
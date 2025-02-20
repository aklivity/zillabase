import { Notify } from 'quasar'

const showSuccess = (message, timeout = 5000) => {
    if (typeof Notify.create === 'function')
        Notify.create({
            type: 'positive',
            message: message,
            position: 'top-right',
            progress: true,
            timeout: timeout,
            actions: [{ icon: 'close', color: 'white' }],
        })
}

const showError = (message, timeout = 5000) => {
    if (typeof Notify.create === 'function')
        Notify.create({
            type: 'negative',
            message: message,
            position: 'top-right',
            progress: true,
            timeout: timeout,
            actions: [{ icon: 'close', color: 'white' }],
        })
}

const showWarning = (message, timeout = 5000) => {
    if (typeof Notify.create === 'function')
        Notify.create({
            type: 'warning',
            message: message,
            position: 'top-right',
            progress: true,
            timeout: timeout,
            actions: [{ icon: 'close', color: 'white' }],
        })
}

export {
    showError,
    showSuccess,
    showWarning
}

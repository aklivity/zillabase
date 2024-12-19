import axios from 'axios'
import app from './app'

const appAddUsers = payload => {
    return new Promise((resolve, reject) => {
        axios.post(`${app.apiEndpoint}/auth/users`, payload)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetUsers = () => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/auth/users`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetUserById = id => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/auth/users/${id}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appDeleteUserById = id => {
    return new Promise((resolve, reject) => {
        axios.delete(`${app.apiEndpoint}/auth/users/${id}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appAddSSOProviders = payload => {
    return new Promise((resolve, reject) => {
        axios.post(`${app.apiEndpoint}/auth/sso/providers`, payload)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetSSOProviders = () => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/auth/sso/providers`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetSSOProvidersById = id => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/auth/sso/providers/${id}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appDeleteSSOProvidersById = id => {
    return new Promise((resolve, reject) => {
        axios.delete(`${app.apiEndpoint}/auth/sso/providers/${id}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

export {
    appAddUsers,
    appGetUsers,
    appGetUserById,
    appDeleteUserById,
    appAddSSOProviders,
    appGetSSOProviders,
    appGetSSOProvidersById,
    appDeleteSSOProvidersById
}
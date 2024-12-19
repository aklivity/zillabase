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

const appGetStorageBuckets = () => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/storage/buckets`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appAddStorageBuckets = bucketName => {
    return new Promise((resolve, reject) => {
        axios.post(`${app.apiEndpoint}/storage/buckets/${bucketName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appDeleteStorageBuckets = bucketName => {
    return new Promise((resolve, reject) => {
        axios.delete(`${app.apiEndpoint}/storage/buckets/${bucketName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetStorageObjects = bucketName => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/storage/objects/${bucketName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appGetStorageObjectDetail = (bucketName, fileName) => {
    return new Promise((resolve, reject) => {
        axios.get(`${app.apiEndpoint}/storage/objects/${bucketName}/${fileName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appAddStorageObject = (bucketName, fileName) => {
    return new Promise((resolve, reject) => {
        axios.post(`${app.apiEndpoint}/storage/objects/${bucketName}/${fileName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appUpdateStorageObject = (bucketName, fileName) => {
    return new Promise((resolve, reject) => {
        axios.put(`${app.apiEndpoint}/storage/objects/${bucketName}/${fileName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

const appDeleteStorageObject = (bucketName, fileName) => {
    return new Promise((resolve, reject) => {
        axios.delete(`${app.apiEndpoint}/storage/objects/${bucketName}/${fileName}`)
            .then(response => {
                resolve(response)
            }).catch(error => {
                reject(error)
            })
    })
}

export {
    appUpdateStorageObject,
    appDeleteStorageObject,
    appAddStorageObject,
    appGetStorageObjects,
    appGetStorageObjectDetail,
    appAddStorageBuckets,
    appDeleteStorageBuckets,
    appGetStorageBuckets,
    appAddUsers,
    appGetUsers,
    appGetUserById,
    appDeleteUserById,
    appAddSSOProviders,
    appGetSSOProviders,
    appGetSSOProvidersById,
    appDeleteSSOProvidersById
}
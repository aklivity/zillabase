const objectToQuery = payload => {
    const searchParams = new URLSearchParams();
    Object.keys(payload).forEach(key => searchParams.append(key, payload[key]));
    return searchParams.toString()
}

export {
    objectToQuery
}
import request from '@/request'

export async function listVersions(
    params: API.listVersionsParams,
    options?: { [key: string]: any }
) {
    return request<API.BaseResponseAppVersionVOList>('/app/version/list', {
        method: 'GET',
        params: {
            ...params,
        },
        ...(options || {}),
    })
}

export async function rollbackVersion(
    params: API.rollbackVersionParams,
    options?: { [key: string]: any }
) {
    return request<API.BaseResponseBoolean>('/app/version/rollback', {
        method: 'POST',
        params: {
            ...params,
        },
        ...(options || {}),
    })
}

export async function compareVersions(
    params: API.compareVersionsParams,
    options?: { [key: string]: any }
) {
    return request<API.BaseResponseAppVersionDiffVO>('/app/version/diff', {
        method: 'GET',
        params: {
            ...params,
        },
        ...(options || {}),
    })
}

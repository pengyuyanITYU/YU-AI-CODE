import request from '@/request';

export const uploadFileApi = async (file: File, bizType = 'userPrompts') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bizType', bizType);
  
  return await request.post<API.FileUploadResponse>('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const processFileApi = async (url: string, fileName: string) => {
  const params = new URLSearchParams();
  params.append('url', url);
  params.append('fileName', fileName);
  
  return await request.post<API.FileProcessResult>(`/file/process?${params.toString()}`);
};

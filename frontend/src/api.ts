export type Profile={id:number;email:string;name:string;department:string;studentNumber:string};
export type Organization={id:number;name:string;department:string;description:string;role:'MEMBER'|'STAFF'|'LEADER'|null};
export class ApiError extends Error {constructor(public status:number,public code:string,message:string){super(message)}}
let csrf:string|undefined;
export async function request<T>(path:string,method='GET',body?:unknown):Promise<T>{
 if(method!=='GET' && !csrf){const token=await fetch('/api/v1/auth/csrf',{credentials:'include'});csrf=(await token.json()).token;}
 const response=await fetch('/api/v1'+path,{method,credentials:'include',headers:{'Content-Type':'application/json',...(csrf?{'X-CSRF-TOKEN':csrf}:{})},...(body!==undefined?{body:JSON.stringify(body)}:{})});
 if(response.status===204){if(path==='/auth/logout')csrf=undefined;return undefined as T;}
 const data=await response.json();if(!response.ok){if(response.status===403)csrf=undefined;throw new ApiError(response.status,data.code,data.message||'요청을 처리하지 못했습니다.');}return data;
}

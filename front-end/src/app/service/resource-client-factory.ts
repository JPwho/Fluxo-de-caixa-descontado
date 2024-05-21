import { AxiosAcaoResourceClient } from "./service";
  import { environment } from "../environments/environment";
  import axios, { AxiosInstance } from "axios";
  
  export class ResourceClientFactory {
    static createAxiosInstance(): AxiosInstance {
      return axios.create();
    }
  
    static acaoResourceClient() {
      return new AxiosAcaoResourceClient(
        environment.url,
        ResourceClientFactory.createAxiosInstance()
      );
    }
  }
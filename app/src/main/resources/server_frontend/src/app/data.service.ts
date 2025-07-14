import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface a_travel_track {
  trackId: number;
  name: string;
}

export interface travel_details {
  locationId: number;
  latitude: number;
  longitude: number;
  time: number;
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  constructor(private http: HttpClient) {}

  getAllTracks(): Observable<a_travel_track[]> {
    const params = new HttpParams();
    return this.http.get<a_travel_track[]>('/general', { params });
  }
  
  getTrackDetails(param: string): Observable<travel_details[]> {
    const params = new HttpParams().set('track_name', param);
    return this.http.get<travel_details[]>('/specific_track', { params });
  }
}
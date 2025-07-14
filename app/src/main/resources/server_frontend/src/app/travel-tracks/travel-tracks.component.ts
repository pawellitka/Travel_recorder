import { Component, OnInit, ChangeDetectorRef, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { DataService, a_travel_track, travel_details } from '../data.service';
@Component({
  selector: 'app-travel-tracks',
  templateUrl: './travel-tracks.component.html',
  styleUrls: ['./travel-tracks.component.css'],
  standalone: true,
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})

export class TravelTracksComponent implements OnInit{
  tracks: a_travel_track[] = [];
  TrackDetails: { [key: number]: travel_details[] } = {};
  expandedTracks: Set<number> = new Set();

  constructor(private dataService: DataService, private changeDetectorRef: ChangeDetectorRef) {}

  ngOnInit() {
    this.dataService.getAllTracks().subscribe((data) => {
      this.tracks = data;
    });
  }
    
  location_formatting(details: travel_details): string {
    let result: string =  details.latitude.toString();
    result += (details.latitude > 0 ? "N" : "S"); 
    result += " " + details.longitude.toString();
    result += (details.longitude > 0 ? "E" : "W"); 
    const date: Date = new Date();
    date.setTime(details.time * 1000);
    return result += " - " +  date.toLocaleString();
  }
  
  toggle(item: a_travel_track): void {
    const id = item.trackId;
    if (this.expandedTracks.has(id)) {
      this.expandedTracks.delete(id);
    } else {
      this.expandedTracks.add(id);
      if (!this.TrackDetails[id]) {
        this.dataService.getTrackDetails(item.name).subscribe(det => {
          this.TrackDetails[id] = det;
        });
      }
    }
  }
}
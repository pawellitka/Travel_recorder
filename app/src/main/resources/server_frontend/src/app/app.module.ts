import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { TopBarComponent } from './top-bar/top-bar.component';
import { TravelTracksComponent } from './travel-tracks/travel-tracks.component';

@NgModule({
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    HttpClientModule,
    AppComponent,
    TopBarComponent,
    TravelTracksComponent
  ],
  declarations: [],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class AppModule { }
import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  
  isSidebarOpen: boolean = false;
  menuCollapsed: boolean = false;

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
    this.menuCollapsed = !this.menuCollapsed; // Alternamos o estado do menuCollapsed ao clicar no botão do menu
  }

}

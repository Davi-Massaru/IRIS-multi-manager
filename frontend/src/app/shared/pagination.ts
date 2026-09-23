import {Component,input,output} from '@angular/core';

@Component({
  selector:'app-pagination',
  template:`
    @if(total()>pageSize()){
      <div class="pagination" role="navigation" aria-label="Table pagination">
        <span>Showing {{start()}}–{{end()}} of {{total()}}</span>
        <div>
          <button type="button" [disabled]="page()<=1" (click)="change(page()-1)">Previous</button>
          <span>Page {{page()}} of {{pageCount()}}</span>
          <button type="button" [disabled]="page()>=pageCount()" (click)="change(page()+1)">Next</button>
        </div>
      </div>
    } @else if(total()>0){
      <p class="pagination-count">Showing {{total()}} {{total()===1?'item':'items'}}</p>
    }
  `
})
export class Pagination {
  total=input.required<number>();
  page=input.required<number>();
  pageSize=input(20);
  pageChange=output<number>();

  pageCount(){return Math.max(1,Math.ceil(this.total()/this.pageSize()));}
  start(){return (this.page()-1)*this.pageSize()+1;}
  end(){return Math.min(this.total(),this.page()*this.pageSize());}
  change(page:number){this.pageChange.emit(Math.min(this.pageCount(),Math.max(1,page)));}
}

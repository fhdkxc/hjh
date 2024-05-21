<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<script type="text/javascript" src="/resources/js/jquery.min.js"></script>
<script>
$(function(){
	
});
</script>
<style>
	#DiaryContainer h3{
		font-size: 2.2rem;
		text-align: center;
		backdrop-filter: blur(4px);
		background-color: rgba(255, 255, 255, 1);
		border-radius: 50px;
		box-shadow: 35px 35px 68px 0px rgba(145, 192, 255, 0.5), inset -8px -8px 16px 0px rgba(145, 192, 255, 0.6), inset 0px 11px 28px 0px rgb(255, 255, 255);
		width: 370px;
		padding-top: 35px;
		padding-bottom: 35px;
		margin: auto;
		margin-bottom: 40px;
	}
	#DiaryContainer{
		min-height: 790px;
	}
	#DiaryContainer .custom-pagination{
		max-width:302px;
		margin: auto;
	}
	#DiaryContainer .custom-pagination .pagination {
		 width: max-content;
	}
	.searchForm{
		height: 40px;
		border: 1px solid #ddd;
		border-radius: 5px;
		padding: 15px 20px;
	}
	
	#searchBtn,#insertBtn{
		background: #333;
		height: 40px;
		border: none;
		padding: 10px 15px;
		border-radius: 10px;
		font-family: 'Pretendard' !important;
		font-weight: 600;
		color: #fff;
	}
	#searchBtn:hover, #insertBtn:hover{
		background: #ffd77a;
		color:#333;
		transition:all 1s;
		font-weight: 700;
	}
	#insertBtn{
		background: #006DF0;
	}
	
	#searchCondition{
		height: 40px;
		border: 1px solid #ddd;
		border-radius: 5px;
		padding-left: 10px;
	}
	
	.custom-datatable-overright table tbody tr.none-tr td:hover{
		background: #fff!imporant;
	}
</style>
<div id="DiaryContainer">
	<div class="sparkline13-list">
		<h3>
			<img src="/resources/images/classRoom/freeBrd/freeBoardTit.png" style="width:50px; display:inline-block; vertical-align:middel;">
				일기장
			<img src="/resources/images/classRoom/freeBrd/freeBoardTitChat.png" style="width:50px; display:inline-block; vertical-align:middel;">		
		</h3>
		<div class="sparkline13-graph">
			<div class="datatable-dashv1-list custom-datatable-overright">
				<div class="bootstrap-table">
					<div class="fixed-table-toolbar">
						<div class="pull-left button">
							<button type="button" id="insertBtn">일기 쓰기</button>
						</div>
						<div class="pull-right search" style="margin-bottom: 20px;">
							<form action="#" method="get">
								<!-- 검색어 시작 -->
								<input class="searchForm" type="text" placeholder="" name="keyword" value="${keyword}">
								<button type="submit" id="searchBtn">검색</button>
								<!-- 검색어 끝 -->
							</form>
						</div>
					</div>
					<div class="fixed-table-container" style="padding-bottom: 0px;">
						<div class="fixed-table-body">
							<table id="table" data-toggle="table" data-pagination="true"
								data-search="true" data-show-columns="true"
								data-show-pagination-switch="true" data-show-refresh="true"
								data-key-events="true" data-show-toggle="true"
								data-resizable="true" data-cookie="true"
								data-cookie-id-table="saveId" data-show-export="true"
								data-click-to-select="true" data-toolbar="#toolbar"
								class="table table-hover JColResizer">
								<thead>
									<tr style="font-size:1rem;">
										<th style="width: 10%;" data-field="id" tabindex="0">
											<div class="th-inner ">제목</div>
											<div class="fht-cell"></div>
										</th>
										<th style="width: 40%;" data-field="name" tabindex="0">
											<div class="th-inner ">제목</div>
											<div class="fht-cell"></div>
										</th>
										<th style="width: 20%;" data-field="email" tabindex="0">
											<div class="th-inner ">제목</div>
											<div class="fht-cell"></div>
										</th>
										<th style="width: 20%;" data-field="phone" tabindex="0">
											<div class="th-inner ">제목</div>
											<div class="fht-cell"></div>
										</th>
										<th style="width: 10%;" data-field="action" tabindex="0">
											<div class="th-inner ">제목</div>
											<div class="fht-cell"></div>
										</th>
									</tr>
								</thead>
								<tbody>
									<tr data-index="0" data-code="code" class="detailGo">
										<td style="">
										</td>
										<td style="">
										</td>
										<td style="">
										</td>
										<td style="">
										</td>
										<td class="datatable-ct" style="">
										</td>
									</tr>
<!-- 												조회 된 내용이 없는 경우 -->
<!-- 											<tr data-index="0" class="none-tr"> -->
<!-- 												<td style="text-align: center;" colspan="5" >조회된 결과가 없습니다.</td> -->
<!-- 											</tr> -->
								</tbody>
							</table>
						</div>
						<!-- 페이징 -->
						<div class="custom-pagination">
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
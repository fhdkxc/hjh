package kr.or.ddit.task.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.task.service.TaskService;
import kr.or.ddit.util.ArticlePage;
import kr.or.ddit.util.service.SessionService;
import kr.or.ddit.vo.AtchFileVO;
import kr.or.ddit.vo.ClasStdntVO;
import kr.or.ddit.vo.MemberVO;
import kr.or.ddit.vo.NoticeVO;
import kr.or.ddit.vo.SchulVO;
import kr.or.ddit.vo.TaskResultVO;
import kr.or.ddit.vo.TaskVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/task")
@Controller
public class TaskController {

	@Autowired
	String uploadFolder;

	@Autowired
	TaskService taskService;
	
	@Autowired
	SessionService sessionService;

	// 과제 게시판 목록
	@GetMapping("/taskList")
	public String taskList(Model model, @RequestParam(value = "clasCode", required = false) String clasCode,
			@RequestParam(value = "currentPage", required = false, defaultValue = "1") int currentPage,
			@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
		log.debug("taskList -> clasCode: " + clasCode);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("currentPage", currentPage);
		map.put("keyword", keyword);

		List<TaskVO> taskVOList = taskService.taskList(map);
		log.debug("taskVOList: " + taskVOList);

		return "class/task";
	}

	// 과제 게시판 목록(Ajax)
	@ResponseBody
	@RequestMapping(value = "/taskListAjax", method = RequestMethod.POST)
	public ArticlePage<TaskVO> taskListAjax(@RequestBody Map<String, Object> map) {
		log.debug("taskListAjax -> map: " + map);

		String clasCode = (String) map.get("clasCode");
		log.debug("taskListAjax -> clasCode: " + clasCode);

		List<TaskVO> taskVOList = this.taskService.taskList(map);
		log.debug("taskVOList: " + taskVOList);

		int total = this.taskService.getTotalTask(map);
		log.debug("taskListAjax -> total: " + total);

		String currentPage = map.get("currentPage").toString();
		String keyword = map.get("keyword").toString();

		ArticlePage<TaskVO> data = new ArticlePage<TaskVO>(total, Integer.parseInt(currentPage), 10, taskVOList, keyword, clasCode);
		String url = "/class/taskList";
		data.setUrl(url);

		return data;
	}

	// 과제 게시글 상세 보기
	@GetMapping("/taskDetail")
	public String taskDetail(Model model, HttpServletRequest request,
			@RequestParam(value = "taskCode", required = false) String taskCode) {
		log.debug("taskDetail -> taskCode: " + taskCode);
		
		TaskVO taskVO = this.taskService.taskDetail(taskCode);
		log.debug("taskDetail -> taskVO: " + taskVO);
		
		// 클래스 세션 처리
		sessionService.setClassSession(request, taskVO.getClasCode());

		String atchFileCode = taskVO.getAtchFileCode();
		List<AtchFileVO> atchFileList = new ArrayList<AtchFileVO>();

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("atchFileCode", atchFileCode);
		map.put("atchFileSn", "");

		// 첨부 파일 리스트 불러오기
		if (atchFileCode != null && atchFileCode.length() > 0) {
			atchFileList = this.taskService.atchFileList(map);
			log.debug("taskDetail -> atchFileList: " + atchFileList);
		}

		// 조회수 업데이트
		int result = this.taskService.updateTaskCnt(taskCode);
		log.debug("taskDetail -> result: " + result);

		// 과제 제출 수
		int inputTaskCount = taskService.getInputTaskCount(taskCode);

		model.addAttribute("taskVO", taskVO);
		model.addAttribute("atchFileList", atchFileList);
		model.addAttribute("inputTaskCount", inputTaskCount);

		return "class/taskDetail";
	}

	// 과제 게시글 등록 폼 띄우기
	@GetMapping("/taskInsertForm")
	public String taskInsertForm(HttpServletRequest request, Model model,
			@RequestParam(value = "clasCode", required = false) String clasCode,
			@RequestParam(value = "taskCode", required = false) String taskCode) {
		log.debug("taskInsertForm: " + clasCode);

		// schulCode 가져오기
		SchulVO schulVO = (SchulVO) request.getSession().getAttribute("SCHOOL_INFO");

		model.addAttribute("schulCode", schulVO.getSchulCode());
//      model.addAttribute("clasCode", clasCode);

		return "class/taskInsertForm";
	}

	// 과제 게시글 등록
	@ResponseBody
	@PostMapping("/taskInsert")
	public String taskInsert(TaskVO taskVO, HttpServletRequest request,
			@RequestParam(value = "clasCode", required = false) String clasCode) {
		log.debug("taskInsert -> taskVO: " + taskVO);
		log.debug("taskInsert -> clasCode: " + clasCode);

		// insert 결과 담을 변수 선언
		int result1 = 0;
		int result2 = 0;

		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		String mberId = loginAccount.getMberId();

		// 업로드한 파일 가져오기
		MultipartFile[] multipartFiles = taskVO.getUploadFiles();
		log.debug("taskInsert -> multipartFiles: " + multipartFiles);

		// 업로드한 파일이 있는 경우에만 업로드 진행
		if (multipartFiles != null && multipartFiles.length > 0) {
			File uploadPath = new File(uploadFolder + "\\task\\");

			if (uploadPath.exists() == false) {
				uploadPath.mkdirs();
			}

			if (taskVO.getClasCode() == null) {
				taskVO.setClasCode(clasCode);
			}

			// 첨부파일코드 새로 생성하기
			String atchFileCode = this.taskService.getAtchFileCode(taskVO.getClasCode());
			log.debug("taskInsert -> atchFileCode: " + atchFileCode);

			List<AtchFileVO> atchFileVOList = new ArrayList<AtchFileVO>();
			int sn = 1;

			for (MultipartFile mf : multipartFiles) {
				try {
					UUID uuid = UUID.randomUUID();

					log.debug("fileName" + uuid.toString() + "_" + mf.getOriginalFilename());
					File fileName = new File(uploadPath, uuid.toString() + "_" + mf.getOriginalFilename());

					AtchFileVO atchFileVO = new AtchFileVO();
					mf.transferTo(fileName); // 파일 서버로 복사
					atchFileVO.setAtchFileCode(atchFileCode);
					atchFileVO.setAtchFileSn(sn++);
					atchFileVO.setAtchFileCours(uuid.toString() + "_" + mf.getOriginalFilename());
					atchFileVO.setAtchFileNm(mf.getOriginalFilename());
					atchFileVO.setAtchFileTy(mf.getContentType());
					atchFileVO.setRegistId(mberId);
					atchFileVOList.add(atchFileVO);
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			}

			log.debug("taskInsert -> atchFileVOList: " + atchFileVOList);

			// 1) 첨부 파일 테이블에 insert
			result2 = this.taskService.atchFileInsert(atchFileVOList);
			log.debug("taskInsert -> result2: " + result2);

			// 2) 과제 테이블에 insert
			taskVO.setAtchFileCode(atchFileCode);
			result1 = this.taskService.taskInsert(taskVO);
			log.debug("파일 있 taskInsert -> taskVO(후): " + taskVO);

		} else { // 업로드한 파일이 없는 경우, 과제 insert만 진행
			taskVO.setAtchFileCode("");
			result1 = this.taskService.taskInsert(taskVO);
			log.debug("파일 없 taskInsert -> result1: " + result1);
			log.debug("파일 없 taskInsert -> taskVO(후): " + taskVO);
		}

		return taskVO.getTaskCode();
	}

	// 과제 게시글 등록 -> 알림 테이블 insert
	@ResponseBody
	@PostMapping("/noticeInsertAll")
	public int noticeInsertAll(@RequestBody Map<String, Object> map, HttpServletRequest request) {
		log.debug("noticeInsertAll -> map: " + map);

		String clasCode = (String) map.get("clasCode");

		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		log.debug("loginAccount -> " + loginAccount);

		String mberId = loginAccount.getMberId();

		// 클래스 내 학생/학부모 리스트
		List<String> noticeRcvIdList = taskService.getAllClassMber(clasCode);
		log.debug("noticeInsertAll -> noticeRcvIdList: " + noticeRcvIdList);
		
		int result = 0;
		
		// 알림 보낼 리스트가 있는 경우, 알림 전송
		if(noticeRcvIdList.size() > 0) {
			map.put("noticeRcvIdList", noticeRcvIdList);
			log.debug("리스트 넣은 후 noticeInsertAll -> map: " + map);
			
			result = taskService.noticeInsertAll(map);
			log.debug("noticeInsertAll -> result: " + result);
		}

		return result;
	}

	// 과제 게시글 수정 폼 띄우기
	@GetMapping("/taskUpdateForm")
	public String taskUpdateForm(Model model, @RequestParam(value = "taskCode", required = false) String taskCode) {
		log.debug("taskUpdateForm -> taskCode: " + taskCode);

		TaskVO taskVO = this.taskService.taskDetail(taskCode);
		log.debug("taskUpdateForm -> taskVO: " + taskVO);

		model.addAttribute("taskVO", taskVO);

		return "class/taskUpdateForm";
	}

	// 과제 게시글 수정
	@ResponseBody
	@PostMapping("/taskUpdate")
	public String taskUpdate(TaskVO taskVO, HttpServletRequest request) {
		log.debug("taskUpdate -> taskVO: " + taskVO);

		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		log.debug("loginAccount -> " + loginAccount);

		String mberId = loginAccount.getMberId();
		log.debug("taskUpdate -> mberId: " + mberId);

		String taskCn = taskVO.getTaskCn().trim().replaceAll("\"", "'");
		taskVO.setTaskCn(taskCn);
		log.debug("taskCn 처리 후 -> taskVO: " + taskVO);

		int result1 = 0;
		int result2 = 0;

		MultipartFile[] multipartFiles = taskVO.getUploadFiles();

		// 기존 글 첨부 파일 코드 가져오기
		String atchFileCode = taskVO.getAtchFileCode();
		log.debug("taskUpdate -> atchFileCode: " + atchFileCode);

		// 새로 업로드한 파일이 있을 때(수정 시)만 업로드 진행
		if (multipartFiles != null && multipartFiles.length > 0) {
			File uploadPath = new File(uploadFolder + "\\task\\");

			if (uploadPath.exists() == false) {
				uploadPath.mkdirs();
			}

			// 기존 글에 첨부 파일이 없는 경우, 첨부 파일 코드 생성하기
			if (taskVO.getAtchFileCode() == null || taskVO.getAtchFileCode().isEmpty()) {
				log.debug(">>>첨부파일코드가없어용");
				atchFileCode = this.taskService.getAtchFileCode(taskVO.getClasCode());
				log.debug(">>>첨부파일코드생성해줬어용: " + atchFileCode);
			}

			// 파일들을 리스트에 담음
			List<AtchFileVO> atchFileVOList = new ArrayList<AtchFileVO>();

			int sn = 1;

			for (MultipartFile mf : multipartFiles) {
				try {
					UUID uuid = UUID.randomUUID();

					log.debug("fileName" + uuid.toString() + "_" + mf.getOriginalFilename());
					File fileName = new File(uploadPath, uuid.toString() + "_" + mf.getOriginalFilename());

					// 파일 서버로 복사
					mf.transferTo(fileName);

					AtchFileVO atchFileVO = new AtchFileVO();
					atchFileVO.setAtchFileCode(atchFileCode);
					atchFileVO.setAtchFileSn(sn++);
					atchFileVO.setAtchFileCours(uuid.toString() + "_" + mf.getOriginalFilename()); // 웹 경로
					atchFileVO.setAtchFileNm(mf.getOriginalFilename()); // 원본 파일 명
					atchFileVO.setAtchFileTy(mf.getContentType());
					atchFileVO.setRegistId(mberId);
					atchFileVO.setUpdtId(mberId);

					atchFileVOList.add(atchFileVO);
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			}

			log.debug("taskUpdate -> atchFileVOList: " + atchFileVOList);

			// 기존 글에 첨부 파일이 없는 경우, insert 진행
			if (taskVO.getAtchFileCode() == null || taskVO.getAtchFileCode().isEmpty()) {
				result2 = this.taskService.atchFileInsert(atchFileVOList);
				log.debug("taskUpdate -> result2: " + result2);

			} else { // 기존 글에 첨부 파일이 있는 경우, 기존 첨부파일을 초기화하고 insert 진행
				int deleteRes = this.taskService.atchFileDelete(atchFileCode);
				log.debug("taskUpdate -> deleteRes: " + deleteRes);
				result2 = this.taskService.atchFileInsert(atchFileVOList);
				log.debug("taskUpdate -> result2: " + result2);
			}

			taskVO.setAtchFileCode(atchFileCode);
			log.debug("taskUpdate -> taskVO(첨부 파일 코드 가져온 후): " + taskVO);
			result1 = this.taskService.taskUpdate(taskVO);

			log.debug("파일 있 taskUpdate -> taskVO(후): " + taskVO);

		} else { // 새로 업로드한 파일이 없을 때
			taskVO.setAtchFileCode(atchFileCode); // 기존 첨부 파일 코드를 그대로 가져감
			result1 = this.taskService.taskUpdate(taskVO);
			log.debug("파일 없 taskUpdate -> result1: " + result1);
		}
		
		// 과제 게시글 제목이 수정된 경우, 알림 제목도 같이 수정
		if(taskVO.getTaskSj() != null) {
			this.taskService.noticeSjUpdate(taskVO);
		}

		return "class/taskUpdate";
	}

	// 과제 게시글 삭제
	@ResponseBody
	@PostMapping("/taskDelete")
	public int taskDelete(@RequestBody TaskVO taskVO) {
		log.debug("taskDelete -> taskVO: " + taskVO);

		String atchFileCode = taskVO.getAtchFileCode();

		// 첨부 파일 있는 경우
		if (atchFileCode != "" || atchFileCode != null) {
			// 첨부 파일 정보 삭제
			int deleteFileRes = this.taskService.atchFileDelete(atchFileCode);
			log.debug("taskDelete -> deleteRes: " + deleteFileRes);
		}

		// 과제 게시글 삭제
		int deleteTaskRes = this.taskService.taskDelete(taskVO.getTaskCode());
		log.debug("taskDelete -> deleteTaskRes: " + deleteTaskRes);
		
		// 과제 게시글 삭제 -> 연관 테이블 데이터 삭제 처리
		// 학생/학부모 알림 삭제
		this.taskService.noticeDeleteAll(taskVO.getTaskCode());
		
		// 제출된 과제 삭제
		this.taskService.inputTaskDelete(taskVO.getTaskCode());
		
		// 제출된 과제의 첨부파일 삭제
		this.taskService.inputTaskAtchFileDelete(taskVO.getTaskCode());

		return deleteTaskRes;
	}

	// 과제 제출 현황 리스트
	@ResponseBody
	@RequestMapping(value = "/inputTaskList", method = RequestMethod.POST)
	public List<ClasStdntVO> inputTaskList(@RequestBody Map<String, Object> map, HttpServletRequest request) {
		String taskCode = (String) map.get("taskCode");
		String mberId = (String) map.get("mberId");
		log.debug("inputTaskList -> taskCode: " + taskCode);
		log.debug("inputTaskList -> mberId: " + mberId);

		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		String role = loginAccount.getVwMemberAuthVOList().get(0).getCmmnDetailCode();
		log.debug("inputTaskList -> role: " + role);

		// 학부모인 경우, 자녀 리스트를 구해서 map에 추가
		if (role.equals("ROLE_A01003")) {
			List<String> childList = taskService.getChildList(loginAccount.getMberId());
			log.debug("getChildList -> childList: " + childList);
			map.put("childList", childList);
		}

		List<ClasStdntVO> clasStdntVOList = this.taskService.inputTaskList(map);
		log.debug("inputTaskList -> clasStdntVOList: " + clasStdntVOList);

		return clasStdntVOList;
	}

	// 과제 제출
	@ResponseBody
	@PostMapping("/inputTask")
	public int inputTask(TaskResultVO taskResultVO, HttpServletRequest request) {
		log.debug("inputTask -> taskResultVO: " + taskResultVO);

		// 로그인한 학생의 아이디 가져오기
		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		log.debug("loginAccount -> " + loginAccount);
		String mberId = loginAccount.getMberId();

		// 로그인한 학생의 반 학생 코드 가져오기;
		String clasStdntCode = this.taskService.getClasStdntCode(mberId);
		taskResultVO.setClasStdntCode(clasStdntCode);
		log.debug("반 학생 코드 넣은 후 -> taskResultVO: " + taskResultVO);

		// 1) 과제 제출(TASK_RESULT 테이블에 INSERT)
		int result1 = this.taskService.inputTask(taskResultVO);
		log.debug("inputTask -> inputTaskRes: " + result1);
		log.debug("inputTask 후 -> taskResultVO" + taskResultVO);

		taskResultVO.setAtchFileCode(taskResultVO.getTaskResultCode());

		// 제출한 과제 가져오기
		MultipartFile multipartFile = taskResultVO.getUploadFile();
		log.debug("inputTask -> multipartFile: " + multipartFile);

		File uploadPath = new File(uploadFolder + "\\task\\result\\");

		if (uploadPath.exists() == false) {
			uploadPath.mkdirs();
		}

		List<AtchFileVO> atchFileVOList = new ArrayList<AtchFileVO>();
		AtchFileVO atchFileVO = new AtchFileVO();

		try {
			UUID uuid = UUID.randomUUID();

			log.debug("inputTask -> fileName: " + uuid.toString() + "_" + multipartFile.getOriginalFilename());
			File fileName = new File(uploadPath, uuid.toString() + "_" + multipartFile.getOriginalFilename());

			// 파일 서버로 복사
			multipartFile.transferTo(fileName);

			atchFileVO.setAtchFileCode(taskResultVO.getAtchFileCode()); // taskResult에 저장된 첨부파일코드를 가져옴
			atchFileVO.setAtchFileSn(1); // 1개의 파일로 과제 제출
			atchFileVO.setAtchFileCours(uuid.toString() + "_" + multipartFile.getOriginalFilename());
			atchFileVO.setAtchFileNm(multipartFile.getOriginalFilename());
			atchFileVO.setAtchFileTy(multipartFile.getContentType());
			atchFileVO.setRegistId(mberId);

			atchFileVOList.add(atchFileVO);

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		log.debug("inputTask -> atchFileVO: " + atchFileVO);

		// 2) 첨부 파일 업로드(ATCH_FILE 테이블에 INSERT)
		int result2 = this.taskService.atchFileInsert(atchFileVOList);
		log.debug("inputTask -> result2: " + result2);

		return result1 + result2;
	}

	// 제출한 과제 수정
	@ResponseBody
	@PostMapping("/myTaskUpdate")
	public int myTaskUpdate(TaskResultVO taskResultVO, HttpServletRequest request) {
		log.debug("myTaskUpdate -> taskResultVO: " + taskResultVO);

		// 로그인한 학생의 아이디 가져오기
		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		log.debug("loginAccount -> " + loginAccount);
		String mberId = loginAccount.getMberId();

		MultipartFile multipartFile = taskResultVO.getUploadFile();
		log.debug("myTaskUpdate -> multipartFile: " + multipartFile);

		File uploadPath = new File(uploadFolder + "\\task\\result\\");

		List<AtchFileVO> atchFileVOList = new ArrayList<AtchFileVO>();
		AtchFileVO atchFileVO = new AtchFileVO();

		try {
			UUID uuid = UUID.randomUUID();

			log.debug("myTaskUpdate -> fileName: " + uuid.toString() + "_" + multipartFile.getOriginalFilename());
			File fileName = new File(uploadPath, uuid.toString() + "_" + multipartFile.getOriginalFilename());

			// 파일 서버로 복사
			multipartFile.transferTo(fileName);

			atchFileVO.setAtchFileCode(taskResultVO.getTaskResultCode());
			atchFileVO.setAtchFileSn(1); // 1개의 파일로 과제 제출
			atchFileVO.setAtchFileCours(uuid.toString() + "_" + multipartFile.getOriginalFilename());
			atchFileVO.setAtchFileNm(multipartFile.getOriginalFilename());
			atchFileVO.setAtchFileTy(multipartFile.getContentType());
			atchFileVO.setUpdtId(mberId);

			atchFileVOList.add(atchFileVO);

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		log.debug("myTaskUpdate -> atchFileVO: " + atchFileVO);

		int result = this.taskService.atchFileUpdate(atchFileVOList);
		log.debug("myTaskUpdate -> result: " + result);

		return result;
	}

	// 제출한 과제 삭제
	@ResponseBody
	@PostMapping("/myTaskDelete")
	public int myTaskDelete(@RequestBody Map<String, Object> map) {
		String taskResultCode = (String) map.get("taskResultCode");
		log.debug("myTaskDelete -> taskResultCode: " + taskResultCode);

		int result1 = this.taskService.myTaskDelete(taskResultCode);
		log.debug("myTaskDelete -> result: " + result1);

		// 첨부 파일 테이블에 있는 데이터도 함께 삭제
		int result2 = this.taskService.atchFileDelete(taskResultCode);

		return result1 + result2;
	}
	
	// 칭찬 스티커 주기
	@ResponseBody
	@PostMapping("/complimentStickerUpdate")
	public int complimentStickerUpdate(String taskResultCode) {
		log.debug("complimentStickerUpdate -> taskResultCode: " + taskResultCode);
		
		int result = taskService.complimentStickerUpdate(taskResultCode);
		log.debug("complimentStickerUpdate -> result: " + result);
		
		return result;
	}

	// 피드백 등록
	@ResponseBody
	@PostMapping("/feedbackInsert")
	public NoticeVO feedbackInsert(HttpServletRequest request, Model model, @RequestBody Map<String, Object> map) {
		log.debug("feedbackInsert -> map: " + map);

		// 피드백 테이블 insert
		int result1 = this.taskService.feedbackInsert(map);
		log.debug("feedbackInsert -> result1: " + result1);

		// 세션에 저장된 로그인 아이디 가져오기
		MemberVO loginAccount = (MemberVO) request.getSession().getAttribute("USER_INFO");
		log.debug("loginAccount -> " + loginAccount);
		String noticeSndId = loginAccount.getMberId();

		map.put("noticeSndId", noticeSndId);
		map.put("noticeCn", "새 피드백이 달렸습니다.");

		// 알림 테이블 insert
		int result2 = taskService.fdbckNoticeInsert(map);
		log.debug("feedbackInsert -> result2: " + result2);

		// 헤더로 보내기 위해 등록한 피드백의 알림 정보를 담음
		NoticeVO noticeVO = taskService.feedbackToHeader(map);
		log.debug("feedbackInsert -> noticeVO: " + noticeVO);

		return noticeVO;
	}

	// 피드백 수정
	@ResponseBody
	@PostMapping("/feedbackUpdate")
	public int feedbackUpdate(@RequestBody Map<String, Object> map) {
		log.debug("feedbackUpdate -> map: " + map);

		// 피드백 테이블 update
		int result = this.taskService.feedbackInsert(map);
		log.debug("feedbackUpdate -> result: " + result);

		return result;
	}

	// 피드백 삭제
	@ResponseBody
	@PostMapping("/feedbackDelete")
	public int feedbackDelete(@RequestBody Map<String, Object> map) {
		log.debug("feedbackDelete -> map: " + map);

		// 피드백 테이블 insert
		int result = this.taskService.feedbackInsert(map);
		log.debug("feedbackDelete -> result: " + result);

		// 피드백 알림도 같이 삭제
		int result2 = taskService.noticeDelete(map);
		log.debug("feedbackDelete -> result2: " + result2);

		return result;
	}

	// 첨부 파일 미리 보기
	@GetMapping("/pdfView")
	public void pdfView(HttpServletResponse response,
			@RequestParam(value = "atchFileCode", required = false) String atchFileCode,
			@RequestParam(value = "atchFileSn", required = false, defaultValue = "1") String atchFileSn)
			throws IOException {

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("atchFileCode", atchFileCode);
		map.put("atchFileSn", atchFileSn);
		log.debug("pdfView -> map: " + map);
		
		// 첨부 파일 리스트 가져오기
		List<AtchFileVO> atchFileList = this.taskService.atchFileList(map);
		log.debug("pdfView -> atchFileList: " + atchFileList);
		
		// 첨부 파일이 없는 경우
		if (atchFileList.isEmpty()) {
			throw new FileNotFoundException("첨부 파일이 없습니다.");
		}

		String pdfFilePath = "";

		for (AtchFileVO atchFileVO : atchFileList) {
			// 과제물인지 과제 제출물인지 구분하여 경로 지정
			if (atchFileCode.contains("TSK")) {
				pdfFilePath = uploadFolder + "\\task\\" + atchFileVO.getAtchFileCours();
			} else {
				pdfFilePath = uploadFolder + "\\task\\result\\" + atchFileVO.getAtchFileCours();
			}

			File pdfFile = new File(pdfFilePath);

			// 파일이 없는 경우
			if (!pdfFile.exists()) {
				throw new IOException("파일을 찾을 수 없습니다.");
			}
			
			// 파일 경로에 들어갈 파일 이름의 특수 문자 및 공백 처리
			String encodedFileName = URLEncoder.encode(atchFileVO.getAtchFileNm(), "UTF-8");

			// HTTP 응답 헤더 설정
			response.setContentType("application/pdf");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"");

			// 파일 읽기 및 전송
			try (FileInputStream fis = new FileInputStream(pdfFile); OutputStream os = response.getOutputStream()) {

				byte[] buffer = new byte[1024];
				int bytesRead;

				while ((bytesRead = fis.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				os.flush();
			} catch (IOException e) {
				throw new IOException("파일을 읽거나 전송하는 중에 오류가 발생했습니다.", e);
			}
		}
	}
}
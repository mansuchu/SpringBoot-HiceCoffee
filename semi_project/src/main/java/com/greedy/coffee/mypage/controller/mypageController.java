package com.greedy.coffee.mypage.controller;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.greedy.coffee.cart.dto.CartProDTO;
import com.greedy.coffee.cart.dto.MyCartDTO;
import com.greedy.coffee.cart.service.CartService;
import com.greedy.coffee.common.Pagenation;
import com.greedy.coffee.common.PagingButtonInfo;
import com.greedy.coffee.member.dto.MemberDTO;
import com.greedy.coffee.member.service.AuthenticationService;
import com.greedy.coffee.mypage.dto.OrderDTO;
import com.greedy.coffee.mypage.service.MypageService;
import com.greedy.coffee.product.dto.ProDTO;
import com.greedy.coffee.qna.dto.QnaDTO;
import com.greedy.coffee.qna.service.QnaService;
import com.greedy.coffee.review.Service.RevBoardService;
import com.greedy.coffee.review.dto.RevBoardDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/mypage")
public class mypageController {

	private final MypageService mypageServcie;
	private final AuthenticationService authenticationService;
	private final MessageSourceAccessor messageSourceAccessor;
	private final RevBoardService revBoardService;
	private final QnaService qnaService;
	private final CartService cartService;

	public mypageController(MypageService mypageServcie, AuthenticationService authenticationService,
			MessageSourceAccessor messageSourceAccessor, QnaService qnaService, RevBoardService revBoardService, CartService cartService) {

		this.mypageServcie = mypageServcie;
		this.authenticationService = authenticationService;
		this.messageSourceAccessor = messageSourceAccessor;
		this.revBoardService = revBoardService;
		this.qnaService = qnaService;
		this.cartService = cartService;
	}

	/* 회원정보수정 화면 이동 */
	@GetMapping("/userpage")
	public String goModifyMember() {

		return "mypage/userpage";
	}

	@PostMapping("/update")
	public String modifyMember(@ModelAttribute MemberDTO updateMember, @AuthenticationPrincipal MemberDTO loginMember,
			RedirectAttributes rttr) {

		updateMember.setMemId(loginMember.getMemId());

		log.info("[[[[1_로그인멤버]]]MemberController] loginMember : {}", loginMember);
		log.info("[[[[2_디비상에서 수정/업데이트멤버]]]MemberController] modifyMember request Member : {}", updateMember); // 디비상의 수정

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 현재저장 정보
		SecurityContextHolder.getContext()
				.setAuthentication(createNewAuthentication(authentication, loginMember.getMemId()));// 데이터수정

		log.info("[[[[3_데이터 수정]]]]MemberController] authentication : {}", authentication);

		mypageServcie.modifyMember(updateMember);

		rttr.addFlashAttribute("message", messageSourceAccessor.getMessage("mypage.modify"));

		return "redirect:/";
	}

	/* 회원 탈퇴 */
	@GetMapping("/delete") 
	public String deleteMember(@AuthenticationPrincipal MemberDTO member, RedirectAttributes rttr) {

		log.info("[MemberController] deleteMember ==========================================================");
		log.info("[MemberController] member : " + member);

		mypageServcie.removeMember(member); 

		SecurityContextHolder.clearContext(); 

		rttr.addFlashAttribute("message", messageSourceAccessor.getMessage("mypage.delete")); 

		log.info(" 탈퇴[MemberController] deleteMember ==========================================================");

		return "redirect:/"; 
	}


	@GetMapping("/mypost")
	public String mypostPage(Model model) {

		// 현재 로그인된 유저 정보 가져오기
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.info("principal {}", auth.getPrincipal());

		Page<QnaDTO> qnaList = null;
		Page<RevBoardDTO> revBoardList = null;
		String searchValue = "";
		int page = 1;

		MemberDTO member = (MemberDTO) auth.getPrincipal();
		qnaList = mypageServcie.selectQnaListInMypage(member);
		revBoardList = mypageServcie.selectRevBoardListInMypage(member);

		PagingButtonInfo revPaging = Pagenation.getPagingButtonInfo(revBoardList);
		PagingButtonInfo qnaPaging = Pagenation.getPagingButtonInfo(qnaList);

		model.addAttribute("revBoardList", revBoardList);
		model.addAttribute("qnaList", qnaList);
		model.addAttribute("revBoardSize", revBoardList.getTotalElements());
		model.addAttribute("qnaListSize", qnaList.getTotalElements());

		return "mypage/mypost";
	}

	@GetMapping("/myorder")
	public String myorderPage(@RequestParam(defaultValue = "1") int page, Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.info("principal {}", auth.getPrincipal());
		MemberDTO member = (MemberDTO) auth.getPrincipal();

		Page<OrderDTO> orderList = mypageServcie.selectOrderListInMypage(page, member);

		PagingButtonInfo orderPaging = Pagenation.getPagingButtonInfo(orderList);

		model.addAttribute("orderList", orderList);
		model.addAttribute("orderPaging", orderPaging);

		model.addAttribute("orderListSize", orderList.getTotalElements());

		log.info("orderList {} paging {} size {}", orderList.getContent(), orderPaging, orderList.getTotalElements());

		return "mypage/myorder";
	}

	@PostMapping("/order/cancel")
	public String cancelOrder(@ModelAttribute OrderDTO orderDTO) {
		// 로그인 검증 & 본인 주문 확인 추가하기
		log.info("[OrderCotroller] =======================================");
		log.info("[order cancel] request : {}", orderDTO);

		if (orderDTO == null || orderDTO.getOrderCode() == null) {
			return "redirect:/";
		}

		mypageServcie.cancelOrder(orderDTO);

		return "redirect:/mypage/myorder";
	}

	@PostMapping("/order/back")
	public String takeBackOrder(@ModelAttribute OrderDTO orderDTO) {
		// 로그인 검증 & 본인 주문 확인 추가하기
		log.info("[OrderCotroller] =======================================");
		log.info("[order takeback] request : {}", orderDTO);

		if (orderDTO == null || orderDTO.getOrderCode() == null) {
			return "redirect:/";
		}

		mypageServcie.takeBackOrder(orderDTO);

		return "redirect:/mypage/myorder";
	}
	
	@GetMapping("/mybag")
	public String cartList(@RequestParam(defaultValue = "1") int page, 
			Model model ) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.info("principal {}", auth.getPrincipal());
		MemberDTO member = (MemberDTO) auth.getPrincipal();
		
		MyCartDTO dto = cartService.selectCartList(page, member.getMemId());
		
		Page<CartProDTO> cartList = dto.getPagingCartProDTO();
		int sum = dto.getSum();
		
		PagingButtonInfo paging = Pagenation.getPagingButtonInfo(cartList);
		
		model.addAttribute("cartList", cartList);
		model.addAttribute("cartListSize", cartList.getTotalElements());
		model.addAttribute("paging", paging);
		model.addAttribute("cartSum", sum);
  
		log.info("cartList is worked ");
		
		return "/mypage/mybag";
		
	}

	/* createNewAuthentication메소드*/
	protected Authentication createNewAuthentication(Authentication currentAuth, String memId) {

		UserDetails newPrincipal = authenticationService.loadUserByUsername(memId);
		UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(newPrincipal,
				currentAuth.getCredentials(), newPrincipal.getAuthorities());
		newAuth.setDetails(currentAuth.getDetails());

		return newAuth;

	}

}

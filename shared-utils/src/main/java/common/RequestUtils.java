package common;

import dto.PageRequestDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class RequestUtils {

	public static Pageable getPageable(PageRequestDto pageRequestDto) {
		return PageRequest.of(pageRequestDto.page(), pageRequestDto.size(), Sort
			.by(Sort.Order.by(pageRequestDto.sortKey()).with(Sort.Direction.fromString(pageRequestDto.sortOrder()))));
	}

}

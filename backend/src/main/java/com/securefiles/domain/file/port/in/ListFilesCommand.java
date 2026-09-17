package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.FileFailureCodes;
import com.securefiles.domain.file.model.list.ListFilesException;

public record ListFilesCommand(int page, int size) {

	public static final int DEFAULT_PAGE = 1;
	public static final int DEFAULT_SIZE = 10;
	public static final int MAX_SIZE = 50;

	public ListFilesCommand() {
		this(DEFAULT_PAGE, DEFAULT_SIZE);
	}

	public ListFilesCommand {
		if (page < 1) {
			throw invalidPagination();
		}
		if (size < 1 || size > MAX_SIZE) {
			throw invalidPagination();
		}
	}

	private static ListFilesException invalidPagination() {
		return new ListFilesException(
				FileFailureCodes.INVALID_PAGINATION,
				"The page or size parameter is invalid.");
	}
}
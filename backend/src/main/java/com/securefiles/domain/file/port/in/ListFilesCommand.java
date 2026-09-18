package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.list.FileListQuery;
import java.util.Objects;

public record ListFilesCommand(FileListQuery query, String requesterId, boolean administrator) {

	public ListFilesCommand() {
		this(new FileListQuery(), null, false);
	}

	public ListFilesCommand(int page, int size) {
		this(new FileListQuery(page, size), null, false);
	}

	public ListFilesCommand(FileListQuery query) {
		this(query, null, false);
	}

	public ListFilesCommand {
		query = Objects.requireNonNull(query, "query must not be null");
		if (requesterId != null && requesterId.isBlank()) {
			throw new IllegalArgumentException("requesterId must not be blank");
		}
	}

	public int page() {
		return query.page();
	}

	public int size() {
		return query.size();
	}

	public FileListQuery listQuery() {
		return query;
	}
}
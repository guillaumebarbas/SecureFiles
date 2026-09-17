package com.securefiles.domain.file.port.in;

import com.securefiles.domain.file.model.list.FileListQuery;
import java.util.Objects;

public record ListFilesCommand(FileListQuery query) {

	public ListFilesCommand() {
		this(new FileListQuery());
	}

	public ListFilesCommand(int page, int size) {
		this(new FileListQuery(page, size));
	}

	public ListFilesCommand {
		query = Objects.requireNonNull(query, "query must not be null");
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
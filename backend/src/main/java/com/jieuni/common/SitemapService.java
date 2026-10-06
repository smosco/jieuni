package com.jieuni.common;

import com.jieuni.maker.MakerRepository;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 공개 페이지가 있는 공간과 만든 사람 전부. 프론트의 app/sitemap.ts가 쓴다.
@Service
@Transactional(readOnly = true)
public class SitemapService {

	private final PlaceRepository places;
	private final MakerRepository makers;

	SitemapService(PlaceRepository places, MakerRepository makers) {
		this.places = places;
		this.makers = makers;
	}

	public SitemapResponse sitemap() {
		return new SitemapResponse(places.findSitemapEntries(PlaceStatus.HAS_PAGE),
				makers.findSitemapEntries(PlaceStatus.HAS_PAGE));
	}

}

package org.egov.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.domain.model.Message;
import org.egov.domain.model.Tenant;
import org.egov.persistence.dto.MessageCacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class MessageCacheRepository {

	private static final String MESSAGES_HASH_KEY = "messages";
	private static final String COMPUTED_MESSAGES_HASH_KEY = "computedMessages";
	private StringRedisTemplate stringRedisTemplate;
	private ObjectMapper objectMapper;
    public static final Logger logger = LoggerFactory.getLogger(MessageCacheRepository.class);

	public MessageCacheRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Computed messages cached for one locale, tenant, and module.
	 *
	 * @param locale requested locale
	 * @param tenant requested tenant
	 * @param module module whose computed list is stored apart from other modules
	 * @return cached messages, or {@code null} on a cache miss
	 */
	public List<Message> getComputedMessages(String locale, Tenant tenant, String module) {
		return getMessages(locale, tenant, module, COMPUTED_MESSAGES_HASH_KEY);
	}

	/**
	 * Stores the computed message list for one module. The Redis field is
	 * {@code locale:tenant:module}, so the cached value is limited to that module.
	 *
	 * @param locale requested locale
	 * @param tenant requested tenant
	 * @param module module this list was computed for
	 * @param messages computed messages to cache
	 */
	public void cacheComputedMessages(String locale, Tenant tenant, String module, List<Message> messages) {
		putMessages(locale, tenant, module, COMPUTED_MESSAGES_HASH_KEY, messages);
	}

	/**
	 * Raw Postgres messages cached for one locale, tenant, and module.
	 *
	 * @param locale message locale
	 * @param tenant tenant the rows belong to
	 * @param module module the rows belong to
	 * @return cached messages, or {@code null} on a cache miss
	 */
	public List<Message> getMessages(String locale, Tenant tenant, String module) {
		return getMessages(locale, tenant, module, MESSAGES_HASH_KEY);
	}

	/**
	 * Stores raw Postgres messages for one module under the field {@code locale:tenant:module}.
	 *
	 * @param locale message locale
	 * @param tenant tenant the rows belong to
	 * @param module module the rows belong to
	 * @param messages rows read from Postgres
	 */
	public void cacheMessages(String locale, Tenant tenant, String module, List<Message> messages) {
		putMessages(locale, tenant, module, MESSAGES_HASH_KEY, messages);
	}

	public void bustCache() {
		stringRedisTemplate.delete(MESSAGES_HASH_KEY);
		bustAllComputedMessagesCache();
	}

	public void bustCacheEntry(String locale, Tenant tenant) {
		bustRawMessagesCacheEntry(locale, tenant);
		bustComputedMessagesCache(locale, tenant);
	}

	private void bustRawMessagesCacheEntry(String locale, Tenant tenant) {
		String messageKey = getKey(locale, tenant.getTenantId());
		getMatchingHashKeys(MESSAGES_HASH_KEY, messageKey)
				.forEach(key -> stringRedisTemplate.opsForHash().delete(MESSAGES_HASH_KEY, key));
	}

	private void bustComputedMessagesCache(String locale, Tenant tenant) {
		if (tenant.isDefaultTenant()) {
			bustAllComputedMessagesCache();
		} else {
			deleteMatchingSubTenantCacheKeys(locale, tenant);
		}
	}

	private void bustAllComputedMessagesCache() {
		stringRedisTemplate.delete(COMPUTED_MESSAGES_HASH_KEY);
	}

	private void deleteMatchingSubTenantCacheKeys(String locale, Tenant tenant) {
		getMatchingSubTenantCacheKeys(locale, tenant).forEach(this::deleteComputedMessageCacheEntry);
	}

	private Long deleteComputedMessageCacheEntry(String cacheKey) {
		return stringRedisTemplate.opsForHash().delete(COMPUTED_MESSAGES_HASH_KEY, cacheKey);
	}

	private Stream<String> getMatchingSubTenantCacheKeys(String locale, Tenant tenant) {
		String parentCacheKey = getKey(locale, tenant.getTenantId());
		return getMatchingHashKeys(COMPUTED_MESSAGES_HASH_KEY, parentCacheKey);
	}

	/**
	 * Hash field names that contain {@code keyFragment}. Uses {@code HKEYS}, which returns
	 * names only, so cache bust does not pull cached message JSON into the heap. A locale
	 * and tenant prefix matches that entry, its per-module fields, and child tenants.
	 *
	 * @param hashKey Redis hash to scan
	 * @param keyFragment locale and tenant prefix, for example {@code en_IN:mh.panvel}
	 * @return matching field names; empty when the hash has none
	 */
	private Stream<String> getMatchingHashKeys(String hashKey, String keyFragment) {
		Set<Object> keys = stringRedisTemplate.opsForHash().keys(hashKey);
		if (keys == null || keys.isEmpty()) {
			return Stream.empty();
		}
		return keys.stream().map(key -> (String) key).filter(key -> key.contains(keyFragment));
	}

	private List<Message> getMessages(String locale, Tenant tenant, String module, String hashKey) {
		String messageKey = getKey(locale, tenant.getTenantId(), module);
		final String entry = (String) stringRedisTemplate.opsForHash().get(hashKey, messageKey);
		if (entry != null) {
			final MessageCacheEntry messageCacheEntry;
			try {
				messageCacheEntry = objectMapper.readValue(entry, MessageCacheEntry.class);
				return messageCacheEntry.getDomainMessages();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private void putMessages(String locale, Tenant tenant, String module, String hashKey, List<Message> messages) {
		String messageKey = getKey(locale, tenant.getTenantId(), module);
		final MessageCacheEntry messageCacheEntry = new MessageCacheEntry(messages);
		try {
			final String cacheEntry = objectMapper.writeValueAsString(messageCacheEntry);
			stringRedisTemplate.opsForHash().put(hashKey, messageKey, cacheEntry);
		} catch (JsonProcessingException e) {
			logger.error("Exception occurred while processing JSON: " + e.getMessage());
		}
	}

	private String getKey(String locale, String tenant) {
		return String.format("%s:%s", locale, tenant);
	}

	/**
	 * Redis hash field for one module, shaped as {@code locale:tenant:module}.
	 *
	 * @param locale message locale
	 * @param tenant tenant id
	 * @param module module name
	 * @return field name used for both raw and computed cache entries
	 */
	private String getKey(String locale, String tenant, String module) {
		return String.format("%s:%s:%s", locale, tenant, module);
	}

}

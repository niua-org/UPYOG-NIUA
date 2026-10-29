package org.egov.domain.service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.egov.domain.model.AuthenticatedUser;
import org.egov.domain.model.Message;
import org.egov.domain.model.MessageIdentity;
import org.egov.domain.model.MessageSearchCriteria;
import org.egov.domain.model.Tenant;
import org.egov.persistence.repository.MessageCacheRepository;
import org.egov.persistence.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Responsible for creating, updating and computing localization message list.
 *
 * For the search request: locale: mr_IN, tenant: mh.panvel the computed message
 * list is based on the below logic - 1) Retrieve messages for locale: en_IN and
 * tenant: default 2) Retrieve messages for locale: mr_IN and tenant mh 3)
 * Override messages of step 1 with messages from step 2. 4) Retrieve messages
 * from locale: mr_IN and tenant mh.panvel 5) Override messages from step 3 with
 * messages from step 4.
 *
 * Messages are loaded and cached per module so a search only holds that module's rows.
 *
 * The items cached in Redis are, for each module - 1) The computed message list.
 * 2) The raw messages read from Postgres for every locale and tenant combination.
 *
 * Cache bust logic - a) For a create/update request to locale: mr_IN and
 * tenant: mh.panvel - 1) In validate cache entry for raw messages with key
 * mr_IN:mh.panvel 2) In validate cache entry for computed messages with key
 * mr_IN:mh.panvel
 *
 * b) For a create/update request to locale: mr_IN and tenant: mh - 1) In
 * validate cache entry for raw messages with key mr_IN:mh 2) In validate cache
 * entry for computed messages with key mr_IN:mh.panvel and mr_IN:mh
 *
 * c) For a create/update request to locale: <locale> and tenant: default- 1) In
 * validate all computed messages entries. 2) In validate cache entry for raw
 * messages with key <locale>:default
 */
@Service
//@Slf4j
public class MessageService {
	private static final String ENGLISH_INDIA = "en_IN";
	private MessageRepository messageRepository;
	private MessageCacheRepository messageCacheRepository;

	public MessageService(MessageRepository messageRepository, MessageCacheRepository messageCacheRepository) {
		this.messageRepository = messageRepository;
		this.messageCacheRepository = messageCacheRepository;
	}

	public void upsert(Tenant tenant, List<Message> messages, AuthenticatedUser user) {

		Message message = messages.get(0);
		messageRepository.upsert(message.getTenant(), message.getLocale(), message.getModule(), messages, user);
		bustCacheEntriesForMessages(tenant, messages);
	}

	public void create(Tenant tenant, List<Message> messages, AuthenticatedUser user) {
		messageRepository.save(messages, user);
		bustCacheEntriesForMessages(tenant, messages);
	}

	public void updateMessagesForModule(Tenant tenant, List<Message> messages, AuthenticatedUser user) {
		if (CollectionUtils.isEmpty(messages)) {
			throw new IllegalArgumentException("update message list cannot be empty");
		}
		final Message message = messages.get(0);
		messageRepository.update(message.getTenant(), message.getLocale(), message.getModule(), messages, user);
		bustCacheEntriesForMessages(tenant, messages);
	}

	public void bustCache() {
		messageCacheRepository.bustCache();
	}

	public List<Message> getFilteredMessages(MessageSearchCriteria searchCriteria) {
		List<Message> messages = new ArrayList<>();
		for (String module : resolveModules(searchCriteria)) {
			for (Message message : getMessagesForModule(searchCriteria, module)) {
				if (matchesSearchCriteria(message, searchCriteria)) {
					messages.add(message);
				}
			}
		}
		messages.sort(Comparator.comparing(Message::getCode));
		return messages;
	}

	public void delete(List<MessageIdentity> messageIdentities) {
		final Map<Tenant, List<MessageIdentity>> tenantToMessageIdentitiesMap = messageIdentities.stream()
				.collect(Collectors.groupingBy(MessageIdentity::getTenant));
		tenantToMessageIdentitiesMap.keySet()
				.forEach(tenant -> deleteMessagesForGivenTenant(tenantToMessageIdentitiesMap, tenant));
	}

	private void bustCacheEntriesForMessages(Tenant tenant, List<Message> messages) {
		final List<MessageIdentity> messageIdentities = messages.stream().map(Message::getMessageIdentity)
				.collect(Collectors.toList());
		bustCacheEntriesForMessageIdentities(tenant, messageIdentities);
	}

	private void bustCacheEntriesForMessageIdentities(Tenant tenant, List<MessageIdentity> messageIdentities) {
		messageIdentities.stream().map(MessageIdentity::getLocale).distinct()
				.forEach(locale -> bustCacheEntry(tenant, locale));
	}

	private void bustCacheEntry(Tenant tenant, String locale) {
		messageCacheRepository.bustCacheEntry(locale, tenant);
	}

	/**
	 * Modules to load for this search. Uses the requested module list when it is present.
	 * When it is absent, reads distinct module names for the tenant hierarchy and locale
	 * (and {@code en_IN}) so each module can be loaded on its own instead of the full catalog.
	 *
	 * @param searchCriteria locale, tenant, and optional comma-separated modules
	 * @return module names to compute; never null
	 */
	private List<String> resolveModules(MessageSearchCriteria searchCriteria) {
		if (!searchCriteria.isModuleAbsent()) {
			return Arrays.stream(searchCriteria.getModule().split("[,]"))
					.map(String::trim)
					.filter(module -> !module.isEmpty())
					.distinct()
					.collect(Collectors.toList());
		}
		List<String> tenantIds = searchCriteria.getTenantId().getTenantHierarchy().stream()
				.map(Tenant::getTenantId)
				.distinct()
				.collect(Collectors.toList());
		List<String> locales = new ArrayList<>();
		locales.add(searchCriteria.getLocale());
		if (!ENGLISH_INDIA.equals(searchCriteria.getLocale())) {
			locales.add(ENGLISH_INDIA);
		}
		return messageRepository.findModules(tenantIds, locales).stream()
				.filter(module -> module != null && !module.isEmpty())
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * Whether a computed message belongs in the response. The locale and tenant must match
	 * the request. When codes are present, the message code must be one of them.
	 *
	 * @param message candidate from the module's computed list
	 * @param searchCriteria search locale, tenant, and optional codes
	 * @return {@code true} when the message should be returned
	 */
	private boolean matchesSearchCriteria(Message message, MessageSearchCriteria searchCriteria) {
		if (!searchCriteria.getLocale().equals(message.getLocale())
				|| !searchCriteria.getTenantId().getTenantId().equals(message.getTenant())) {
			return false;
		}
		return CollectionUtils.isEmpty(searchCriteria.getCodes()) || searchCriteria.getCodes().contains(message.getCode());
	}

	/**
	 * Computed messages for a single module. Returns the per-module Redis entry when present,
	 * and otherwise merges Postgres rows for that module and caches the result. Holding one
	 * module at a time keeps the search heap bounded by that module rather than the catalog.
	 *
	 * @param searchCriteria locale and tenant used as the cache identity
	 * @param module module to load
	 * @return computed messages for that module, including English fallbacks for missing codes
	 */
	private List<Message> getMessagesForModule(MessageSearchCriteria searchCriteria, String module) {
		final List<Message> cachedMessages = messageCacheRepository.getComputedMessages(searchCriteria.getLocale(),
				searchCriteria.getTenantId(), module);
		if (cachedMessages != null) {
			return cachedMessages;
		}
		final List<Message> computedMessages = computeMessageList(searchCriteria.getLocale(),
				searchCriteria.getTenantId(), module);
		messageCacheRepository.cacheComputedMessages(searchCriteria.getLocale(), searchCriteria.getTenantId(), module,
				computedMessages);
		return computedMessages;
	}

	private void deleteMessagesForGivenTenant(Map<Tenant, List<MessageIdentity>> tenantToMessageIdentitiesMap,
			Tenant tenant) {
		final List<MessageIdentity> messageIdentitiesForGivenTenant = tenantToMessageIdentitiesMap.get(tenant);
		final Map<String, List<MessageIdentity>> localeToMessageIdentitiesMap = messageIdentitiesForGivenTenant.stream()
				.collect(Collectors.groupingBy(MessageIdentity::getLocale));
		localeToMessageIdentitiesMap.keySet()
				.forEach(locale -> deleteMessagesForGivenLocale(tenant, localeToMessageIdentitiesMap, locale));
		bustCacheEntriesForMessageIdentities(tenant, messageIdentitiesForGivenTenant);
	}

	private void deleteMessagesForGivenLocale(Tenant tenant,
			Map<String, List<MessageIdentity>> localeToMessageIdentitiesMap, String locale) {
		final List<MessageIdentity> messageIdentitiesForGivenLocale = localeToMessageIdentitiesMap.get(locale);
		final Map<String, List<MessageIdentity>> moduleToMessageIdentitiesMap = messageIdentitiesForGivenLocale.stream()
				.collect(Collectors.groupingBy(MessageIdentity::getModule));
		moduleToMessageIdentitiesMap.keySet()
				.forEach(module -> deleteMessagesForGivenModule(tenant, locale, moduleToMessageIdentitiesMap, module));
	}

	private void deleteMessagesForGivenModule(Tenant tenant, String locale,
			Map<String, List<MessageIdentity>> moduleToMessageIdentitiesMap, String module) {
		final List<MessageIdentity> messageIdentitiesForGivenModule = moduleToMessageIdentitiesMap.get(module);
		final List<String> codes = getCodesForGivenMessage(messageIdentitiesForGivenModule);
		messageRepository.delete(tenant.getTenantId(), locale, module, codes);
	}

	private List<String> getCodesForGivenMessage(List<MessageIdentity> messageIdentitiesForGivenModule) {
		return messageIdentitiesForGivenModule.stream().map(MessageIdentity::getCode).collect(Collectors.toList());
	}

	private List<Message> computeMessageList(String locale, Tenant tenant, String module) {
		final Collection<Message> messagesForGivenLocale = getMessagesForGivenLocale(locale, tenant, module);
		List<Message> defaultMessages = getDefaultMessagesForMissingCodes(messagesForGivenLocale, module);
		return Stream.concat(messagesForGivenLocale.stream(), defaultMessages.stream())
				.sorted(Comparator.comparing(Message::getCode)).collect(Collectors.toList());
	}

	private List<Message> getDefaultMessagesForMissingCodes(Collection<Message> messagesForGivenLocale, String module) {
		final List<Message> messagesInEnglishForDefaultTenant = fetchMessageFromRepository(ENGLISH_INDIA,
				new Tenant(Tenant.DEFAULT_TENANT), module);

        Set<String> messageCodesInGivenLanguage = new HashSet<>();

        messagesForGivenLocale.forEach(message -> {
            messageCodesInGivenLanguage.add(message.getModule()+message.getCode());
        });

		return getEnglishMessagesForCodesNotPresentInLocalLanguage(messageCodesInGivenLanguage,
				messagesInEnglishForDefaultTenant);
	}

	private Collection<Message> getMessagesForGivenLocale(String locale, Tenant tenant, String module) {
		final Map<String, Message> codeToMessageMap = new HashMap<>();
		final List<Message> messages = tenant.getTenantHierarchy().stream()
				.map(tenantItem -> fetchMessageFromRepository(locale, tenantItem, module)).flatMap(List::stream)
				.collect(Collectors.toList());

		messages.forEach(message -> {
			final Message matchingMessage = codeToMessageMap.get(message.getModule()+message.getCode());
			if (matchingMessage == null) {
				codeToMessageMap.put(message.getModule()+message.getCode(), message);
			} else {
				if (message.isMoreSpecificComparedTo(matchingMessage)) {
					codeToMessageMap.put(message.getModule()+message.getCode(), message);
				}
			}
		});

		return codeToMessageMap.values();
	}

	private List<Message> getEnglishMessagesForCodesNotPresentInLocalLanguage(Set<String> messageCodesForGivenLocale,
			List<Message> messagesInEnglish) {
		return messagesInEnglish.stream().filter(message -> !messageCodesForGivenLocale.contains(message.getModule()+message.getCode()))
				.collect(Collectors.toList());
	}

	private List<Message> fetchMessageFromRepository(String locale, Tenant tenant, String module) {
		final List<Message> cachedMessages = messageCacheRepository.getMessages(locale, tenant, module);
		if (cachedMessages != null) {
			return cachedMessages;
		}
		List<Message> messages = messageRepository.findByTenantIdLocaleAndModule(tenant, locale, module);
		if (messages == null) {
			messages = Collections.emptyList();
		}
		messageCacheRepository.cacheMessages(locale, tenant, module, messages);
		return messages;
	}

}

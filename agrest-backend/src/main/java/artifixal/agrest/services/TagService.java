package artifixal.agrest.services;

import artifixal.agrest.dto.TagDTO;
import artifixal.agrest.entity.Tag;
import artifixal.agrest.entity.TargetTag;
import artifixal.agrest.entity.TargetTagKey;
import artifixal.agrest.repository.TagRepository;
import artifixal.agrest.repository.TargetTagRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service used for tag managment.
 */
@Service
@AllArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TargetTagRepository targetTagRepository;

    /**
     * Connects tags with given target.
     *
     * @param targetID Tag owner.
     * @param tags Possible variants.
     *
     * @return A flux with many-to-many entity.
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Flux<TargetTag> addTagsToTarget(Long targetID, Collection<String> tags) {
        return addOrGet(tags)
            .map((tag) -> new TargetTag(new TargetTagKey(targetID, tag.getId())))
            .collectList()
            .flatMapMany((tagList) -> targetTagRepository.saveAll(tagList));
    }

    /**
     * Updates target tags removing deleted ones and adding new.
     *
     * @param targetID What to update.
     * @param tagsFromEntity Existing tags. To what to compare.
     * @param tagsFromDTO Updated tags. From what to compare.
     *
     * @return A flux with inserted many-to-many entity.
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Flux<TargetTag> editTargetTags(Long targetID, Collection<Tag> tagsFromEntity,
        Collection<String> tagsFromDTO) {
        Set<String> existingTags = tagsFromEntity.stream()
            .map(Tag::getName)
            .collect(Collectors.toSet());
        Set<String> newTags = tagsFromDTO.stream()
            .filter((tag) -> !existingTags.contains(tag))
            .collect(Collectors.toSet());
        Set<TargetTagKey> deletedTags = tagsFromEntity.stream()
            .filter((tag) -> !tagsFromDTO.contains(tag.getName()))
            .map((tag) -> new TargetTagKey(targetID, tag.getId()))
            .collect(Collectors.toSet());
        return Flux.fromIterable(deletedTags)
            .flatMap((targetTag) -> targetTagRepository.deleteById(targetTag))
            .thenMany(addTagsToTarget(targetID, newTags));
    }

    /**
     * Retrieves existing tags all at once or creates absent one using batch insert.
     *
     * @param tags What to look for.
     *
     * @return A flux with all tags.
     */
    public Flux<Tag> addOrGet(Collection<String> tags) {
        List<String> tagsNormalized = tags.stream()
            .map((tag) -> tag.toLowerCase())
            .collect(Collectors.toList());
        return tagRepository.findAllByNameIn(tagsNormalized)
            .collectMap(Tag::getName)
            .flatMapMany((existingTags) -> {
                // Collect all new tags
                Flux newTagFlux = Flux.fromIterable(tags)
                    .filter((tag) -> !existingTags.containsKey(tag))
                    .map((newTag) -> new Tag(newTag))
                    .collectList()
                    // Save new tags
                    .flatMapMany((newTags) -> {
                        if (newTags.isEmpty())
                            return Flux.empty();
                        return tagRepository.saveAll(newTags);
                    });
                // Concat existing and new tags
                return Flux.concat(Flux.fromIterable(existingTags.values()), newTagFlux);
            });
    }

    /**
     * Retrieves tag or creates one if absent.
     *
     * @param tagName What to look for.
     *
     * @return A Mono with tag.
     */
    public Mono<Tag> addOrGet(String tagName) {
        String tagNameNormalized = tagName.toLowerCase();
        return tagRepository.findByName(tagNameNormalized)
            .switchIfEmpty(addNewTag(tagNameNormalized));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    protected Mono<Tag> addNewTag(String tagName) {
        Tag tag = new Tag(tagName);
        return tagRepository.save(tag);
    }

    public Flux<TagDTO> getTagPage(Pageable page) {
        return tagRepository.findByOrderByName(page)
            .map((tag) -> new TagDTO(Optional.of(tag.getId()), tag.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Long> deleteOrphanedTags() {
        return tagRepository.deleteAllOrphaned();
    }

    public List<Tag> toEntityList(Collection<TagDTO> tags) {
        return tags.stream()
            .map((tag) -> new Tag(tag.id().orElse(null), tag.name()))
            .toList();
    }

    public List<TagDTO> toDTOList(Collection<Tag> tags) {
        return tags.stream()
            .map((tag) -> new TagDTO(Optional.of(tag.getId()), tag.getName()))
            .toList();
    }
}

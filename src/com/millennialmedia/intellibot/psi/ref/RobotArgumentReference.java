package com.millennialmedia.intellibot.psi.ref;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.millennialmedia.intellibot.ide.config.RobotOptionsProvider;
import com.millennialmedia.intellibot.psi.element.Argument;
import com.millennialmedia.intellibot.psi.element.Import;
import com.millennialmedia.intellibot.psi.element.KeywordStatement;
import com.millennialmedia.intellibot.psi.util.PerformanceCollector;
import com.millennialmedia.intellibot.psi.util.PerformanceEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Scott Albertine
 */
public class RobotArgumentReference extends PsiReferenceBase<Argument> {

    public RobotArgumentReference(@NotNull Argument element) {
        this(element, false);
    }

    private RobotArgumentReference(Argument element, boolean soft) {
        super(element, soft);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiElement parent = getElement().getParent();
        // TODO: potentially unsafe cast
        PerformanceCollector debug = new PerformanceCollector((PerformanceEntity) getElement(), "resolve");
        PsiElement result = null;
        // we only want to attempt to resolve a resource/library for the first argument
        if (parent instanceof Import) {
            PsiElement secondChild = getSecondChild(parent);
            if (secondChild == getElement()) {
                Import importElement = (Import) parent;
                if (importElement.isResource()) {
                    result = resolveResource();
                } else if (importElement.isLibrary() || importElement.isVariables()) {
                    result = resolveLibrary();
                }
                //} else {
                //result = resolveVariable();
            }
        } else if (parent instanceof KeywordStatement) {
            PsiElement reference = resolveKeyword();
            result = reference == null ? resolveVariable() : reference;
        }
        debug.complete();
        return result;
    }

    private static PsiElement getSecondChild(PsiElement element) {
        if (element == null) {
            return null;
        }
        PsiElement[] children = element.getChildren();
        return children.length > 0 ? children[0] : null;
    }

    private PsiElement resolveKeyword() {
        Argument element = getElement();
        String keyword = element.getPresentableText();
        // all files we import are based off the file we are currently in
        return ResolverUtils.resolveKeywordFromFile(keyword, element.getContainingFile());
    }

    private PsiElement resolveVariable() {
        String text = getElement().getPresentableText();
        PsiElement parent = getElement().getParent();
        PsiElement results = ResolverUtils.resolveVariableFromStatement(text, parent,
                RobotOptionsProvider.getInstance(getElement().getProject()).allowGlobalVariables());
        if (results != null) {
            return results;
        }
        PsiFile file = getElement().getContainingFile();
        return ResolverUtils.resolveVariableFromFile(text, file);
    }

    @Nullable
    private PsiElement resolveLibrary() {
        return RobotFileManager.findPython(getElement().getPresentableText(),
                getElement().getProject(), getElement());
    }

    private PsiElement resolveResource() {
        return RobotFileManager.findRobot(getElement().getPresentableText(),
                getElement().getProject(), getElement());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }
}

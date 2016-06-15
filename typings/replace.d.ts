interface ReplaceOptions {
	regex: string;
	replacement: string;
	paths: Array<string>;
	recursive: boolean;
	silent: boolean;
}

declare module "replace" {
	function replace(options: ReplaceOptions): void;

	export = replace;
}